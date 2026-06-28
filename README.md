# MacroAuto

Ứng dụng tự động hoá thao tác (auto-macro) cho Android, lấy cảm hứng từ lõi của Macrorify.
Không cần root.

## Chức năng (MVP)
- AccessibilityService phát lại thao tác **chạm** bằng cách dispatch gesture (không root).
- Nút nổi overlay: **● REC** (ghi), **▶ PLAY** (phát, lặp 5 lần), **■ STOP** (dừng).
- Ghi chuỗi điểm chạm kèm thời gian trễ, lưu thành JSON.

## Cách dùng
1. Mở app → cấp quyền **hiển thị trên ứng dụng khác** (overlay).
2. Bật **Accessibility** cho MacroAuto.
3. Bấm **Bật nút nổi** → dùng REC/PLAY/STOP.

## Build
Tự động build APK qua GitHub Actions (tab **Actions** → artifact `MacroAuto-debug-apk`).
Hoặc mở bằng Android Studio và Run.

## Giới hạn MVP
- Hiện chỉ ghi/phát thao tác chạm (chưa có vuốt nhiều ngón, dò ảnh/màu, OCR).
- Ghi điểm bằng lớp overlay (khi REC, chạm được lớp này ghi lại; lúc PLAY thì gesture đi vào app thật).
