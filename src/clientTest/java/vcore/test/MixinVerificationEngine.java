package vcore.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public final class MixinVerificationEngine {
   private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
   private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";
   private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
   private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
   private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
   private static final String REDIRECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
   private static final String MODIFY_ARG_DESC = "Lorg/spongepowered/asm/mixin/injection/ModifyArg;";
   private static final String MODIFY_VAR_DESC = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";
   private static final String MODIFY_CONST_DESC = "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";

   public record VerificationResult(
      int totalMixins,
      int verifiedTargets,
      int verifiedInjections,
      int verifiedAccessors,
      int verifiedInvokers,
      int verifiedShadows,
      List<String> errors,
      List<String> details
   ) {
      public boolean isSuccess() {
         return errors.isEmpty();
      }
   }

   public static VerificationResult runVerification() {
      List<String> errors = new ArrayList<>();
      List<String> details = new ArrayList<>();

      int totalMixins = 0;
      int verifiedTargets = 0;
      int verifiedInjections = 0;
      int verifiedAccessors = 0;
      int verifiedInvokers = 0;
      int verifiedShadows = 0;

      System.out.println("================================================================================");
      System.out.println(">>> STARTING VCORE MIXIN VERIFICATION ENGINE (MINECRAFT 1.21.11 VANILLA) <<<");
      System.out.println("================================================================================");

      ClassLoader loader = MixinVerificationEngine.class.getClassLoader();
      JsonObject root;
      try (InputStream in = loader.getResourceAsStream("Vcore.mixins.json")) {
         if (in == null) {
            throw new IllegalStateException("Could not locate Vcore.mixins.json on classpath!");
         }
         root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (Exception e) {
         errors.add("Failed to read Vcore.mixins.json: " + e.getMessage());
         return new VerificationResult(0, 0, 0, 0, 0, 0, errors, details);
      }

      String pkg = root.has("package") ? root.get("package").getAsString().replace('.', '/') : "vcore/injection";
      List<String> mixinList = new ArrayList<>();
      if (root.has("client") && root.get("client").isJsonArray()) {
         for (JsonElement el : root.getAsJsonArray("client")) {
            mixinList.add(el.getAsString().replace('.', '/'));
         }
      }
      if (root.has("mixins") && root.get("mixins").isJsonArray()) {
         for (JsonElement el : root.getAsJsonArray("mixins")) {
            mixinList.add(el.getAsString().replace('.', '/'));
         }
      }

      totalMixins = mixinList.size();
      System.out.printf("[MixinVerifier] Loaded %d mixins from Vcore.mixins.json\n", totalMixins);

      Map<String, ClassNode> targetCache = new HashMap<>();

      for (String mixinRel : mixinList) {
         String classResource = pkg + "/" + mixinRel + ".class";
         ClassNode mixinNode = readClassNode(loader, classResource);
         if (mixinNode == null) {
            String err = "[FILE_MISSING] Mixin class resource not found: " + classResource;
            errors.add(err);
            System.err.println(err);
            continue;
         }

         AnnotationNode mixinAnno = findAnnotation(mixinNode.invisibleAnnotations, mixinNode.visibleAnnotations, MIXIN_DESC);
         if (mixinAnno == null) {
            String err = "[NO_ANNOTATION] Missing @Mixin annotation in bytecode: " + classResource;
            errors.add(err);
            System.err.println(err);
            continue;
         }

         List<String> targetClassNames = extractTargets(mixinAnno);
         if (targetClassNames.isEmpty()) {
            String err = "[NO_TARGETS] @Mixin has no targets declared: " + classResource;
            errors.add(err);
            System.err.println(err);
            continue;
         }

         for (String targetInternal : targetClassNames) {
            if (isOptionalModTarget(targetInternal)) {
               details.add(String.format("Mixin '%s' -> Optional Mod '%s' (SKIPPED)", mixinRel, targetInternal));
               continue;
            }

            ClassNode targetNode = loadTargetWithHierarchy(loader, targetInternal, targetCache);
            if (targetNode == null) {
               String err = String.format("[CLASS_MISSING] Mixin '%s' targets non-existent Minecraft class: '%s'", mixinRel, targetInternal);
               errors.add(err);
               System.err.println(err);
               continue;
            }

            verifiedTargets++;
            details.add(String.format("Mixin '%s' -> Target '%s' (PASS)", mixinRel, targetInternal));

            Set<String> allMethods = collectAllMethods(loader, targetNode, targetCache);
            Set<String> allFields = collectAllFields(loader, targetNode, targetCache);

            // 1. Check Methods in Mixin
            for (MethodNode mNode : mixinNode.methods) {
               // @Inject
               AnnotationNode inject = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, INJECT_DESC);
               if (inject != null) {
                  List<String> targetMethods = extractStringList(inject, "method");
                  for (String targetM : targetMethods) {
                     verifiedInjections++;
                     String cleanName = cleanMethodName(targetM);
                     if (cleanName.equals("<init>")) {
                        if (!hasConstructor(targetNode)) {
                           String err = String.format("[CONSTRUCTOR_MISSING] Mixin '%s' targets <init> on '%s', but target has no constructor!", mixinRel, targetInternal);
                           errors.add(err);
                           System.err.println(err);
                        }
                     } else if (!allMethods.contains(cleanName)) {
                        String err = String.format("[METHOD_MISSING] Mixin '%s' @Inject targets non-existent method '%s' on '%s'!", mixinRel, cleanName, targetInternal);
                        errors.add(err);
                        System.err.println(err);
                     }
                  }
               }

               // @Redirect
               AnnotationNode redirect = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, REDIRECT_DESC);
               if (redirect != null) {
                  List<String> targetMethods = extractStringList(redirect, "method");
                  for (String targetM : targetMethods) {
                     String cleanName = cleanMethodName(targetM);
                     if (!cleanName.equals("<init>") && !allMethods.contains(cleanName)) {
                        String err = String.format("[METHOD_MISSING] Mixin '%s' @Redirect targets non-existent method '%s' on '%s'!", mixinRel, cleanName, targetInternal);
                        errors.add(err);
                        System.err.println(err);
                     }
                  }
               }

               // @ModifyArg
               AnnotationNode modifyArg = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, MODIFY_ARG_DESC);
               if (modifyArg != null) {
                  List<String> targetMethods = extractStringList(modifyArg, "method");
                  for (String targetM : targetMethods) {
                     String cleanName = cleanMethodName(targetM);
                     if (!cleanName.equals("<init>") && !allMethods.contains(cleanName)) {
                        String err = String.format("[METHOD_MISSING] Mixin '%s' @ModifyArg targets non-existent method '%s' on '%s'!", mixinRel, cleanName, targetInternal);
                        errors.add(err);
                        System.err.println(err);
                     }
                  }
               }

               // @ModifyVariable
               AnnotationNode modifyVar = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, MODIFY_VAR_DESC);
               if (modifyVar != null) {
                  List<String> targetMethods = extractStringList(modifyVar, "method");
                  for (String targetM : targetMethods) {
                     String cleanName = cleanMethodName(targetM);
                     if (!cleanName.equals("<init>") && !allMethods.contains(cleanName)) {
                        String err = String.format("[METHOD_MISSING] Mixin '%s' @ModifyVariable targets non-existent method '%s' on '%s'!", mixinRel, cleanName, targetInternal);
                        errors.add(err);
                        System.err.println(err);
                     }
                  }
               }

               // @ModifyConstant
               AnnotationNode modifyConst = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, MODIFY_CONST_DESC);
               if (modifyConst != null) {
                  List<String> targetMethods = extractStringList(modifyConst, "method");
                  for (String targetM : targetMethods) {
                     String cleanName = cleanMethodName(targetM);
                     if (!cleanName.equals("<init>") && !allMethods.contains(cleanName)) {
                        String err = String.format("[METHOD_MISSING] Mixin '%s' @ModifyConstant targets non-existent method '%s' on '%s'!", mixinRel, cleanName, targetInternal);
                        errors.add(err);
                        System.err.println(err);
                     }
                  }
               }

               // @Accessor
               AnnotationNode accessor = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, ACCESSOR_DESC);
               if (accessor != null) {
                  verifiedAccessors++;
                  String fieldName = extractSingleString(accessor, "value");
                  if (fieldName == null || fieldName.isEmpty()) {
                     fieldName = deriveFieldName(mNode.name);
                  }
                  if (!allFields.contains(fieldName)) {
                     String err = String.format("[FIELD_MISSING] Mixin '%s' @Accessor targets non-existent field '%s' on '%s'!", mixinRel, fieldName, targetInternal);
                     errors.add(err);
                     System.err.println(err);
                  }
               }

               // @Invoker
               AnnotationNode invoker = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, INVOKER_DESC);
               if (invoker != null) {
                  verifiedInvokers++;
                  String targetMethod = extractSingleString(invoker, "value");
                  if (targetMethod == null || targetMethod.isEmpty()) {
                     targetMethod = deriveInvokerName(mNode.name);
                  }
                  if (!allMethods.contains(targetMethod)) {
                     String err = String.format("[METHOD_MISSING] Mixin '%s' @Invoker targets non-existent method '%s' on '%s'!", mixinRel, targetMethod, targetInternal);
                     errors.add(err);
                     System.err.println(err);
                  }
               }

               // @Shadow Method
               AnnotationNode shadowMethod = findAnnotation(mNode.invisibleAnnotations, mNode.visibleAnnotations, SHADOW_DESC);
               if (shadowMethod != null) {
                  verifiedShadows++;
                  if (!allMethods.contains(mNode.name)) {
                     String err = String.format("[SHADOW_METHOD_MISSING] Mixin '%s' @Shadow targets non-existent method '%s' on '%s'!", mixinRel, mNode.name, targetInternal);
                     errors.add(err);
                     System.err.println(err);
                  }
               }
            }

            // 2. Check Fields in Mixin (@Shadow Fields)
            for (FieldNode fNode : mixinNode.fields) {
               AnnotationNode shadowField = findAnnotation(fNode.invisibleAnnotations, fNode.visibleAnnotations, SHADOW_DESC);
               if (shadowField != null) {
                  verifiedShadows++;
                  if (!allFields.contains(fNode.name)) {
                     String err = String.format("[SHADOW_FIELD_MISSING] Mixin '%s' @Shadow targets non-existent field '%s' on '%s'!", mixinRel, fNode.name, targetInternal);
                     errors.add(err);
                     System.err.println(err);
                  }
               }
            }
         }
      }

      System.out.println("================================================================================");
      System.out.println(">>> MIXIN VERIFICATION SUMMARY REPORT <<<");
      System.out.println("================================================================================");
      System.out.printf("Total Mixin Classes:     %d\n", totalMixins);
      System.out.printf("Target Classes Verified: %d\n", verifiedTargets);
      System.out.printf("Injections Verified:     %d\n", verifiedInjections);
      System.out.printf("Accessors Verified:      %d\n", verifiedAccessors);
      System.out.printf("Invokers Verified:       %d\n", verifiedInvokers);
      System.out.printf("Shadows Verified:        %d\n", verifiedShadows);
      System.out.printf("Total Failures / Errors: %d\n", errors.size());
      System.out.println("================================================================================");

      writeReportFile(totalMixins, verifiedTargets, verifiedInjections, verifiedAccessors, verifiedInvokers, verifiedShadows, errors, details);

      return new VerificationResult(totalMixins, verifiedTargets, verifiedInjections, verifiedAccessors, verifiedInvokers, verifiedShadows, errors, details);
   }

   private static ClassNode readClassNode(ClassLoader loader, String resourcePath) {
      try (InputStream in = loader.getResourceAsStream(resourcePath)) {
         if (in == null) return null;
         ClassReader cr = new ClassReader(in);
         ClassNode cn = new ClassNode();
         cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
         return cn;
      } catch (Exception e) {
         return null;
      }
   }

   private static ClassNode loadTargetWithHierarchy(ClassLoader loader, String targetInternal, Map<String, ClassNode> cache) {
      if (cache.containsKey(targetInternal)) {
         return cache.get(targetInternal);
      }
      String res = targetInternal + ".class";
      ClassNode cn = readClassNode(loader, res);
      if (cn != null) {
         cache.put(targetInternal, cn);
      }
      return cn;
   }

   private static boolean hasConstructor(ClassNode cn) {
      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("<init>")) return true;
      }
      return false;
   }

   private static Set<String> collectAllMethods(ClassLoader loader, ClassNode start, Map<String, ClassNode> cache) {
      Set<String> set = new HashSet<>();
      ClassNode curr = start;
      Set<String> visited = new HashSet<>();
      while (curr != null && !curr.name.equals("java/lang/Object") && visited.add(curr.name)) {
         for (MethodNode mn : curr.methods) {
            set.add(mn.name);
         }
         if (curr.interfaces != null) {
            for (String iface : curr.interfaces) {
               ClassNode ifaceNode = loadTargetWithHierarchy(loader, iface, cache);
               if (ifaceNode != null) {
                  for (MethodNode mn : ifaceNode.methods) {
                     set.add(mn.name);
                  }
               }
            }
         }
         curr = curr.superName != null ? loadTargetWithHierarchy(loader, curr.superName, cache) : null;
      }
      return set;
   }

   private static Set<String> collectAllFields(ClassLoader loader, ClassNode start, Map<String, ClassNode> cache) {
      Set<String> set = new HashSet<>();
      ClassNode curr = start;
      Set<String> visited = new HashSet<>();
      while (curr != null && !curr.name.equals("java/lang/Object") && visited.add(curr.name)) {
         for (FieldNode fn : curr.fields) {
            set.add(fn.name);
         }
         curr = curr.superName != null ? loadTargetWithHierarchy(loader, curr.superName, cache) : null;
      }
      return set;
   }

   private static AnnotationNode findAnnotation(List<AnnotationNode> inv, List<AnnotationNode> vis, String desc) {
      AnnotationNode an = findInList(inv, desc);
      if (an != null) return an;
      return findInList(vis, desc);
   }

   private static AnnotationNode findInList(List<AnnotationNode> list, String desc) {
      if (list == null) return null;
      for (AnnotationNode an : list) {
         if (desc.equals(an.desc)) return an;
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   private static List<String> extractTargets(AnnotationNode mixinAnno) {
      List<String> targets = new ArrayList<>();
      if (mixinAnno.values == null) return targets;
      for (int i = 0; i < mixinAnno.values.size(); i += 2) {
         String key = (String) mixinAnno.values.get(i);
         Object val = mixinAnno.values.get(i + 1);
         if ("value".equals(key) && val instanceof List<?> list) {
            for (Object obj : list) {
               if (obj instanceof Type t) {
                  targets.add(t.getInternalName());
               }
            }
         } else if ("targets".equals(key) && val instanceof List<?> list) {
            for (Object obj : list) {
               if (obj instanceof String s) {
                  targets.add(s.replace('.', '/'));
               }
            }
         }
      }
      return targets;
   }

   @SuppressWarnings("unchecked")
   private static List<String> extractStringList(AnnotationNode an, String keyName) {
      List<String> result = new ArrayList<>();
      if (an.values == null) return result;
      for (int i = 0; i < an.values.size(); i += 2) {
         String key = (String) an.values.get(i);
         Object val = an.values.get(i + 1);
         if (keyName.equals(key)) {
            if (val instanceof List<?> list) {
               for (Object o : list) {
                  if (o instanceof String s) result.add(s);
               }
            } else if (val instanceof String s) {
               result.add(s);
            }
         }
      }
      return result;
   }

   private static String extractSingleString(AnnotationNode an, String keyName) {
      if (an.values == null) return null;
      for (int i = 0; i < an.values.size(); i += 2) {
         String key = (String) an.values.get(i);
         Object val = an.values.get(i + 1);
         if (keyName.equals(key) && val instanceof String s) {
            return s;
         }
      }
      return null;
   }

   private static boolean isOptionalModTarget(String target) {
      String lower = target.toLowerCase();
      return lower.contains("sodium") || lower.contains("modmenu") || lower.contains("terraformersmc") || lower.contains("authlib");
   }

   private static String cleanMethodName(String raw) {
      int paren = raw.indexOf('(');
      return paren != -1 ? raw.substring(0, paren).trim() : raw.trim();
   }

   private static String deriveFieldName(String methodName) {
      if (methodName.startsWith("get") && methodName.length() > 3) {
         return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
      }
      if (methodName.startsWith("set") && methodName.length() > 3) {
         return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
      }
      if (methodName.startsWith("is") && methodName.length() > 2) {
         return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
      }
      return methodName;
   }

   private static String deriveInvokerName(String methodName) {
      if (methodName.startsWith("invoke") && methodName.length() > 6) {
         return Character.toLowerCase(methodName.charAt(6)) + methodName.substring(7);
      }
      if (methodName.startsWith("call") && methodName.length() > 4) {
         return Character.toLowerCase(methodName.charAt(4)) + methodName.substring(5);
      }
      return methodName;
   }

   private static void writeReportFile(
      int totalMixins,
      int targets,
      int injects,
      int accessors,
      int invokers,
      int shadows,
      List<String> errors,
      List<String> details
   ) {
      try {
         File reportDir = new File("build/reports");
         reportDir.mkdirs();
         File reportFile = new File(reportDir, "mixin-verification-report.txt");
         try (FileWriter writer = new FileWriter(reportFile, StandardCharsets.UTF_8)) {
            writer.write("================================================================================\n");
            writer.write("VCORE MINECRAFT 1.21.11 VANILLA MIXIN VERIFICATION REPORT\n");
            writer.write("================================================================================\n");
            writer.write(String.format("Status:                  %s\n", errors.isEmpty() ? "PASSED (100% VALID)" : "FAILED"));
            writer.write(String.format("Total Mixin Classes:     %d\n", totalMixins));
            writer.write(String.format("Target Classes Verified: %d\n", targets));
            writer.write(String.format("Injections Verified:     %d\n", injects));
            writer.write(String.format("Accessors Verified:      %d\n", accessors));
            writer.write(String.format("Invokers Verified:       %d\n", invokers));
            writer.write(String.format("Shadows Verified:        %d\n", shadows));
            writer.write(String.format("Errors / Missing:        %d\n\n", errors.size()));

            if (!errors.isEmpty()) {
               writer.write("--- DETAILED ERRORS ---\n");
               for (String err : errors) {
                  writer.write(err + "\n");
               }
               writer.write("\n");
            }

            writer.write("--- VERIFIED TARGET DETAILS ---\n");
            for (String det : details) {
               writer.write(det + "\n");
            }
         }
         System.out.println("[MixinVerifier] Full verification report saved to: " + reportFile.getAbsolutePath());
      } catch (Exception e) {
         System.err.println("[MixinVerifier] Failed to write report file: " + e.getMessage());
      }
   }

   public static void main(String[] args) {
      VerificationResult result = runVerification();
      if (!result.isSuccess()) {
         System.err.println("[MixinVerifier] Verification failed with " + result.errors().size() + " errors!");
         System.exit(1);
      } else {
         System.out.println("[MixinVerifier] Verification passed with 0 errors!");
         System.exit(0);
      }
   }
}
