# Lịch Việc — Ứng dụng Android quản lý công việc & thời gian có AI

## Các tính năng chính

- **Giao diện tối Đen – Trắng – Xám hòa hợp**: Tối ưu cho màn hình AMOLED, tương phản sắc nét, gọn gàng không rối mắt.
- **Widget màn hình chính (Ghi chú nhanh)**: Xem tiến độ + các việc sắp làm hôm nay ngay ngoài màn hình chính, có sẵn 2 nút **+ Note AI** (mở thẳng tab AI Ghi chú) và **+ Thêm việc**.
- **Thông báo trên màn hình khóa**:
  - Ghim sẵn danh sách việc chưa làm / sắp làm hôm nay trên màn hình khóa (chế độ công khai `VISIBILITY_PUBLIC`) kèm nút mở nhanh **+ Note AI** và **+ Thêm việc** (bật/tắt trong tab **Thống kê**).
  - Nhắc nhở trước giờ công việc hiện rõ trên màn hình khóa.
- **Lịch theo Ngày & Tuần**: Quản lý 3 trạng thái (Chưa làm, Sắp làm, Đã làm), việc lặp mỗi ngày / theo thứ, giờ cố định vs. giờ linh hoạt (tự động xếp khe trống), Pomodoro và sao lưu JSON.

## Cách thêm Widget ra màn hình chính Android

1. Cài xong file `app-debug.apk` và mở ứng dụng 1 lần để cấp quyền Thông báo.
2. Nhấn giữ vùng trống trên màn hình chính điện thoại → chọn **Tiện ích (Widgets)** → tìm **Lịch Việc — Ghi chú nhanh** và kéo ra màn hình chính.
3. Trên máy Vivo (OriginOS), nếu thông báo màn hình khóa bị ẩn nội dung, vào **Cài đặt → Thông báo → Lịch Việc → Thông báo trên màn hình khóa → Hiện tất cả nội dung**.

## Cách build file APK bằng GitHub Actions

```zsh
cd ~/Desktop/LichViecAndroid
git init
git add .
git commit -m "Update monochrome UI, widget, and lockscreen notifications"
git branch -M main
git remote add origin https://github.com/TEN_TAI_KHOAN/lich-viec-android.git
git push -u origin main
```

Sau khi push, vào tab **Actions** trên GitHub → mở **Build Android APK** → tải **LichViec-debug-apk** (`app-debug.apk`).
