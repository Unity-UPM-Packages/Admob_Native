### **Kế hoạch tổng thể: Xây dựng Plugin Native Ad cho iOS (bằng Swift) tích hợp vào Unity**

**Mục tiêu:** Tạo ra phiên bản iOS cho thư viện quảng cáo Native, có chức năng tương đương với phiên bản Android (bao gồm logic Decorator, timer, pause/resume), và tích hợp liền mạch vào kiến trúc C# đa nền tảng đã có.

---

### **Giai đoạn I: Xây dựng "Động cơ" Native bên phía Xcode (sử dụng Swift)**

**Sản phẩm cuối cùng:** Một thư mục `AdmobNative.framework`.

| **Đầu mục công việc** | **Chi tiết hành động** | **File/Thành phần liên quan** | **Vai trò & Ý nghĩa** |
| :--- | :--- | :--- | :--- |
| **1. Chuẩn bị Môi trường** | - Sử dụng máy tính **macOS**.<br>- Cài đặt **Xcode** và **CocoaPods**. | `Xcode`, `Terminal` | Thiết lập "xưởng sản xuất" cần thiết. |
| **2. Tạo Project & Target** | - Tạo một project **App** mới trong Xcode (ví dụ: `AdmobNativeTestApp`), chọn ngôn ngữ là **Swift**.<br>- Thêm một **Target** mới loại **Framework** (ví dụ: `AdmobNative`), cũng chọn ngôn ngữ là **Swift**. | `AdmobNativeTestApp` (Target), `AdmobNative` (Target) | Tạo "sân thử nghiệm" (App) và "sản phẩm" (Framework) trong cùng một môi trường Swift. |
| **3. Tích hợp AdMob SDK** | - Tạo `Podfile` ở thư mục gốc.<br>- Thêm `pod 'Google-Mobile-Ads-SDK', '11.13.0'` cho cả hai target.<br>- Chạy `pod install`. Luôn mở project bằng file `.xcworkspace`. | `Podfile`, `.xcworkspace` | Cung cấp thư viện AdMob cho cả hai target. |
| **4. Thiết kế Giao diện** | - Trong target `AdmobNative`, tạo một file **View** (`.xib`), ví dụ: `MediumNativeAdLayout.xib`.<br>- Đặt **Class** của View gốc trong `.xib` là `GADNativeAdView`.<br>- Sử dụng **Interface Builder** và **Auto Layout** để thiết kế giao diện.<br>- Gán **`Tag`** (số nguyên) cho mỗi thành phần UI theo bảng quy ước đã thống nhất (Headline=101, MediaView=103...). | `MediumNativeAdLayout.xib` | Tạo ra "tấm toan" giao diện có thể tái sử dụng, không ràng buộc cứng với code. |
| **5. Xây dựng cấu trúc Decorator (Swift)** | - Tạo `protocol IShowBehavior`.<br>- Tạo `class BaseShowBehavior` conform (tuân thủ) protocol `IShowBehavior`.<br>- Tạo các `class CountdownDecorator`, `PositionDecorator`... conform `IShowBehavior`. | `IShowBehavior.swift`<br>`BaseShowBehavior.swift`<br>`CountdownDecorator.swift`<br>`PositionDecorator.swift` | **Sao chép kiến trúc Decorator từ Kotlin sang Swift.** `BaseShowBehavior` xử lý UI cơ bản, các `Decorator` "bọc" thêm chức năng. |
| **6. Tái tạo Logic Timer** | - Bên trong `CountdownDecorator.swift`, sử dụng class **`Timer`** của Swift để tái tạo lại chuỗi 3 timer (initial delay -> countdown -> clickable delay).<br>- Implement logic pause/resume cho các `Timer` này. | `CountdownDecorator.swift` | Dịch lại logic timer từ `SonicCountDownTimer`/`Handler` của Kotlin sang `Timer` của Swift. |
| **7. Viết "Người lắp ráp"** | - Tạo class `AdmobNativeController.swift`.<br>- Class này sẽ quản lý `GADAdLoader`, nhận các cấu hình (`with...`), và lắp ráp chuỗi decorator trong hàm `showAd`. | `AdmobNativeController.swift` | "Bộ não" logic chính, tương đương với `AdmobNativeController.kt`. |
| **8. Xây dựng "Cây cầu" (Swift sang C)** | - Tạo một file `AdmobNativeBridge.swift`.<br>- File này sẽ chứa các hàm `public`. Mỗi hàm sẽ được đánh dấu bằng thuộc tính **`@_cdecl("FunctionName")`** để "export" ra một tên C-style không bị biến tấu.<br>- Sử dụng kiến trúc **Handle/Pointer** (`UnsafeMutableRawPointer`) và một `Dictionary` tĩnh để quản lý nhiều instance của `AdmobNativeController`. | `AdmobNativeBridge.swift` | **"Cửa ngõ" giao tiếp duy nhất** giữa C# và Swift. Che giấu toàn bộ sự phức tạp của Swift. |
| **9. Test độc lập** | - Trong `ViewController.swift` của target App, viết code để gọi các hàm `@_cdecl` từ Bridge.<br>- Viết các hàm Swift để làm callback "giả" và truyền con trỏ của chúng vào Bridge.<br>- Build và chạy target App trên Simulator/thiết bị thật để debug. | `ViewController.swift` | Đảm bảo "động cơ" Swift hoạt động hoàn hảo trước khi lắp ráp vào Unity. |
| **10. Build Framework** | - Trong Xcode, chọn Scheme là `AdmobNative`.<br>- Chọn "Any iOS Device (arm64)".<br>- Build project (Product -> Build).<br>- Lấy thư mục `AdmobNative.framework` từ thư mục "Products". | `AdmobNative.framework` | Đóng gói toàn bộ code Swift, header, và tài nguyên (`.xib`) thành một sản phẩm duy nhất. |

---

### **Giai đoạn II: Tích hợp vào Unity và Hoàn thiện C#**

**Sản phẩm:** Cập nhật package UPM để hỗ trợ đầy đủ cho iOS.

| **Đầu mục công việc** | **Chi tiết hành động** | **File/Thành phần liên quan** | **Vai trò & Ý nghĩa** |
| :--- | :--- | :--- | :--- |
| **11. Import Framework** | - Kéo toàn bộ thư mục `AdmobNative.framework` vào `Packages/Ads Manager/Runtime/Plugins/iOS/` trong project Unity. | `AdmobNative.framework` | Đưa "động cơ" iOS vào trong project Unity. |
| **12. Tạo Client cho iOS** | - Tạo file `AdmobNativePlatforIOSClient.cs` trong thư mục `Runtime/` của package.<br>- Cho nó implement `IAdmobNativePlatformClient`.<br>- Chứa một biến `private IntPtr _nativeControllerPtr;`. | `AdmobNativePlatforIOSClient.cs` | "Người phiên dịch" chuyên biệt cho iOS. |
| **13. Khai báo `[DllImport]`** | - Bên trong `AdmobNativePlatforIOSClient`, khai báo tất cả các hàm đã được "export" bằng `@_cdecl` từ `AdmobNativeBridge.swift` bằng cách sử dụng `[DllImport("__Internal")]`. | `AdmobNativePlatforIOSClient.cs` | Tạo các "nút bấm" trên "điều khiển từ xa" C# để gọi đến Bridge. |
| **14. Xử lý Callbacks** | - Trong `AdmobNativePlatforIOSClient.cs`, định nghĩa các `delegate` có chữ ký khớp với các `typedef` bên Objective-C/Swift.<br>- Viết các phương thức `static` được đánh dấu bằng **`[MonoPInvokeCallback]`**.<br>- Bên trong `Initialize()`, gọi hàm `AdmobNative_RegisterCallbacks(...)` từ Bridge để gửi các con trỏ hàm sang phía native. | `AdmobNativePlatforIOSClient.cs` | Xây dựng cơ chế để Swift có thể "gọi điện" ngược lại cho C#. |
| **15. Implement Interface** | - Hoàn thiện các phương thức của `IAdmobNativePlatformClient` trong `AdmobNativePlatforIOSClient.cs` bằng cách gọi đến các hàm `[DllImport]` tương ứng. | `AdmobNativePlatforIOSClient.cs` | Hoàn thành vai trò "người phiên dịch". |
| **16. Cập nhật "Nhà máy"** | - Mở `AdmobNativePlatform.cs`.<br>- Hoàn thiện khối `#elif UNITY_IOS && !UNITY_EDITOR` để nó tạo `new AdmobNativePlatforIOSClient()`. | `AdmobNativePlatform.cs` | Dạy cho "nhà máy" cách "sản xuất" client cho iOS. |
| **17. Cập nhật Script Post-Build (Rất quan trọng)** | - Mở script Editor `IOSBuildPostProcessor.cs`.<br>- Sử dụng API `UnityEditor.iOS.Xcode.PBXProject` để sửa đổi project Xcode được Unity export ra.<br>- **Nhiệm vụ:**<br>  1. Tự động thêm các framework hệ thống cần thiết.<br>  2. Thêm cờ linker `-ObjC`.<br>  3. **Thêm dòng code để đặt `ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES` thành `YES`**. | `IOSBuildPostProcessor.cs` | Tự động hóa các bước cấu hình bắt buộc cho iOS, đặc biệt là việc đóng gói thư viện Swift. |
| **18. Build và Test** | - Chuyển nền tảng trong Unity sang iOS.<br>- Build project.<br>- Mở project Xcode được tạo ra.<br>- Build và chạy trên thiết bị thật để kiểm tra lần cuối. | `Unity`, `Xcode` | Kiểm tra toàn bộ hệ thống hoạt động cùng nhau trên một thiết bị iOS thật. |


### **Bảng quy ước `Tag` cho các thành phần UI trong `.xib`**

| Tag ID | Thành phần Giao diện | Loại View iOS | Tương ứng `android:id` | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **101** | **Headline** (Tiêu đề) | `UILabel` | `headline` | Thành phần quan trọng nhất. |
| **102** | **Body** (Nội dung mô tả) | `UILabel` | `body` | |
| **103** | **Media View** (Video/Ảnh chính) | `GADMediaView` | `media_view` | Bắt buộc phải có để hiển thị media. |
| **104** | **Icon** (Biểu tượng ứng dụng) | `UIImageView` | `icon` | |
| **105** | **Call to Action** (Nút hành động) | `UIButton` | `cta` | Nút "Cài đặt", "Tìm hiểu thêm"... |
| **106** | **Star Rating** (Đánh giá sao) | `UIImageView` | `rating_bar` | **Lưu ý:** iOS không có RatingBar, dùng UIImageView để hiển thị ảnh sao. |
| **107** | **Advertiser** (Nhà quảng cáo) | `UILabel` | `advertiser` | Thường là tên công ty/ứng dụng. |
| **108** | **Store** (Cửa hàng) | `UILabel` | `ad_store` | Ví dụ: "App Store". |
| **109** | **Price** (Giá) | `UILabel` | `ad_price` | |
| | | | | |
| **110** | **Close Button** (Nút X) | `UIImageView` | `ad_close_button` | Dùng `UIImageView` để dễ dàng tùy chỉnh icon. |
| **111** | **Countdown Text** (Số đếm ngược) | `UILabel` | `ad_countdown_text` | Số "5", "4", "3"... |
| **112** | **Progress Bar** (Vòng tròn/Thanh đếm) | `UIProgressView` | `ad_progress_bar` | Hoặc một `UIView` tùy chỉnh nếu bạn tự vẽ. |

---

**Cách sử dụng:**

*   **Khi thiết kế `.xib`:** Mở **Attributes Inspector** cho mỗi thành phần và nhập số `Tag` tương ứng.
*   **Khi viết code `populate` trong Objective-C:** Dùng `[adView viewWithTag:101]` để lấy `UILabel` của headline, `[adView viewWithTag:103]` để lấy `GADMediaView`, v.v.
