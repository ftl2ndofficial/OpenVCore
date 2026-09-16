package vcore.core.manager.player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.IManager;

public class FriendManager implements IManager {
   public static List<String> friends = new ArrayList<>();

   public boolean isFriend(String name) {
      return friends.stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
   }

   public boolean isFriend(@NotNull Player player) {
      return this.isFriend(player.getName().getString());
   }

   public void removeFriend(String name) {
      friends.remove(name);
   }

   public void addFriend(String friend) {
      friends.add(friend);
   }

   public List<String> getFriends() {
      return friends;
   }

   public void clear() {
      friends.clear();
   }

   public List<AbstractClientPlayer> getNearFriends() {
      return mc.level == null ? new ArrayList<>() : mc.level.players().stream().filter(player -> friends.contains(player.getName().getString())).toList();
   }

   public void saveFriends() {
      File file = new File("Vcore/misc/friends.txt");

      try {
         file.createNewFile();
      } catch (Exception var6) {
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
         for (String friend : friends) {
            writer.write(friend + "\n");
         }
      } catch (Exception var8) {
      }
   }

   public void loadFriends() {
      try {
         File file = new File("Vcore/misc/friends.txt");
         if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
               while (reader.ready()) {
                  friends.add(reader.readLine());
               }
            }
         }
      } catch (Exception var7) {
      }
   }
}
