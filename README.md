# Lịch Việc — Ứng dụng Android quản lý công việc & thời gian có AI

## Các tính năng chính

- **Giao diện tối giản Đen – Trắng – Xám (OLED Timeline)**: Trục thời gian dọc với 1 thẻ tiêu điểm nền trắng chữ đen cho việc sắp làm ngay.
- **Widget màn hình chính (Ghi chú nhanh)**: Xem tiến độ + việc hôm nay, mở nhanh **+ Note AI** và **+ Thêm việc**.
- **Thông báo trên màn hình khóa**: Ghim danh sách việc hôm nay (`VISIBILITY_PUBLIC`) kèm nút **+ Note AI** và nhắc trước giờ.
- **Cài đè cập nhật không mất dữ liệu**: Tự động tăng `versionCode` theo mỗi lần build trên GitHub Actions và ký bằng keystore cố định `app/lichviec-release.jks`.

## Thiết lập chữ ký cố định để cài đè cập nhật (chỉ làm 1 lần)

Nếu repo GitHub của bạn để **Private**, chạy 1 lần trên Mac Terminal:

```zsh
keytool -genkeypair -v -keystore ~/Desktop/LichViecAndroid/app/lichviec-release.jks -alias lichviec -keyalg RSA -keysize 2048 -validity 10000 -storepass lichviec2026 -keypass lichviec2026 -dname "CN=LichViec, OU=Personal, O=LichViec, L=Hanoi, ST=Hanoi, C=VN"
```

Sau đó đẩy lên GitHub:

```zsh
cd ~/Desktop/LichViecAndroid && git add . && git commit -m "Enable fixed signing for in-place updates" && git push
```

Nếu repo để **Public**, không đẩy file `.jks` lên git mà mã hóa Base64 (`base64 -i ~/Desktop/LichViecAndroid/app/lichviec-release.jks | pbcopy`) rồi dán vào GitHub → **Settings → Secrets and variables → Actions → New repository secret** với tên `KEYSTORE_BASE64`.
