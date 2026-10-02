# OpenVCore (1.21.11)

Free VCore for anyone who thinks this thing is overpriced.  
*Bản port miễn phí cho những ai thấy client này thực sự bị hét giá quá cao.*

---

## English

### Build
Requires **JDK 21+**.

- Windows:
  ```cmd
  .\gradlew.bat build -x test
  ```
- Linux / macOS:
  ```bash
  ./gradlew build -x test
  ```

Output jar: `build/libs/OpenVCore-1.0.jar`

### Usage
- Put the jar into `.minecraft/mods/` (requires Fabric Loader `>=0.16.0` on Minecraft 1.21.11).
- **ClickGUI**: `RIGHT SHIFT`
- **Commands**: Prefix `@` in chat (e.g. `@help`, `@bind`)

---

## Tiếng Việt

### Cách build
Yêu cầu **Java 21 trở lên**.

- Windows:
  ```cmd
  .\gradlew.bat build -x test
  ```
- Linux / macOS:
  ```bash
  ./gradlew build -x test
  ```

File sau khi build: `build/libs/OpenVCore-1.0.jar`

### Cách dùng
- Thả file jar vào thư mục `.minecraft/mods/` (cần Fabric Loader `>=0.16.0` trên Minecraft 1.21.11).
- **Mở ClickGUI**: Nhấn phím `RIGHT SHIFT` (Shift phải).
- **Lệnh**: Dùng tiền tố `@` trong khung chat (ví dụ: `@help`, `@bind`).
