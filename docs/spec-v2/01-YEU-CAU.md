# EDUCMIND — YÊU CẦU NGHIỆP VỤ (Spec v2)

> Nguồn: file "Báo cáo" của nhóm (mục II. USER STORY + III. CƠ SỞ DỮ LIỆU).
> Tài liệu này là **bản chuyển ngữ sạch** (file gốc lưu ở bảng mã VNI-Windows cũ nên một số dấu bị mất).
> Script DDL đầy đủ: [`database/schema_v2.sql`](../../database/schema_v2.sql).
> Đối chiếu với code hiện tại: [`03-GAP-ANALYSIS.md`](./03-GAP-ANALYSIS.md).

## 1. Phạm vi

MVP gồm **29 user story** phủ trọn chuỗi: dạy → học → đánh giá → phân tích → gợi ý.

- `US-T*` — giáo viên (TEACHER)
- `US-S*` — học sinh (STUDENT)
- `US-G*` — dùng chung cho cả hai vai trò
- `US-H*` — xử lý tự động của hệ thống

## 2. Đối tượng sử dụng

| Vai trò | Mô tả | Chức năng chính |
|---|---|---|
| **Giáo viên (TEACHER)** — môn Tin học THPT | Phụ trách một hoặc nhiều lớp | Quản lý lớp, khóa học, nội dung giảng dạy; tạo bài tập, bài kiểm tra, chấm điểm; theo dõi — phân tích kết quả; dùng AI tạo câu hỏi và duyệt nội dung |
| **Học sinh (STUDENT)** | Học sinh THPT thuộc lớp do giáo viên phụ trách | Xem bài học, làm và nộp bài; theo dõi kết quả bản thân; nhận gợi ý học tập phù hợp |

> **Chỉ có 2 vai trò.** Không còn `SUPER_ADMIN`, `SCHOOL_ADMIN` như thiết kế cũ.

## 3. Thuật ngữ (bắt buộc dùng thống nhất trong code và UI)

| Thuật ngữ | Ý nghĩa |
|---|---|
| **Mục tiêu học tập** (learning objective) | Kiến thức/kỹ năng học sinh cần đạt sau bài học |
| **Chủ đề** (course topic) | Phần chia nhỏ của khóa học, gom các bài học liên quan. Cấu trúc: **Khóa học → Chủ đề → Bài học** |
| **Phạm vi hoạt động** (scope) | Cấp mà bài tập/bài kiểm tra thuộc về: khóa học, chủ đề hoặc bài học — **mỗi hoạt động chỉ thuộc đúng một cấp** |
| **Bằng chứng học tập** (evidence) | Kết quả học sinh đạt được sau khi hoàn thành bài tập hoặc bài kiểm tra |
| **Mức nắm vững** (mastery) | Mức độ học sinh đáp ứng một mục tiêu, xác định từ kết quả các bài đánh giá liên quan |
| **Lỗ hổng kiến thức** (knowledge gap) | Mục tiêu mà kết quả cho thấy còn hạn chế, cần ôn tập/bổ sung |

## 4. User story dùng chung

### US-G01 — Đăng nhập, đăng xuất
Là người dùng (giáo viên/học sinh), tôi muốn đăng nhập và đăng xuất để sử dụng hệ thống an toàn theo vai trò của mình.
- **AC-G01.1** Đăng nhập đúng thông tin thì truy cập được hệ thống; sai thông tin thì hiển thị thông báo lỗi.
- **AC-G01.2** Người dùng chỉ được truy cập các chức năng thuộc vai trò của mình.
- **AC-G01.3** Sau khi đăng xuất hoặc phiên hết hạn, người dùng phải đăng nhập lại để tiếp tục sử dụng.

### US-G02 — Quên và đặt lại mật khẩu
- **AC-G02.1** Email tồn tại và không tồn tại đều nhận được thông báo xác nhận giống nhau (chống dò email).
- **AC-G02.2** Mỗi mã đặt lại mật khẩu hợp lệ chỉ được sử dụng một lần.
- **AC-G02.3** Khi tạo mã mới, các mã cũ chưa sử dụng đều bị vô hiệu.
- **AC-G02.4** Mật khẩu mới phải đáp ứng độ dài tối thiểu theo cấu hình.

### US-G03 — Nhận thông báo trong hệ thống
- **AC-G03.1** Hệ thống tự động gửi thông báo khi có sự kiện liên quan: học sinh nộp bài, giáo viên xuất bản nội dung, chấm điểm, AI tạo câu hỏi hoàn tất hoặc thất bại, và có gợi ý học tập mới.
- **AC-G03.2** Khi mở thông báo, hệ thống cập nhật trạng thái để xem và thời điểm xem.
- **AC-G03.3** Người dùng chỉ được xem thông báo của chính mình.
- **AC-G03.4** Thông báo liên kết đến nội dung đã xóa phải hiển thị trạng thái nội dung không còn khả dụng.

### US-G04 — Đổi mật khẩu
- **AC-G04.1** Đổi mật khẩu thành công thì đăng nhập được bằng mật khẩu mới và không thể sử dụng mật khẩu cũ.
- **AC-G04.2** Nhập sai mật khẩu hiện tại thì hệ thống không thực hiện đổi mật khẩu.

## 5. Nhóm 1 — Quản lý lớp học và nội dung giảng dạy

### US-T00 — Tạo lớp và thêm học sinh
- **AC-T00.1** Tạo lớp thành công và giáo viên tạo lớp được xác định là người phụ trách.
- **AC-T00.2** Thêm học sinh bằng email; nếu chưa có tài khoản thì hệ thống tạo tài khoản và gửi đường dẫn đặt mật khẩu.
- **AC-T00.3** Không tạo trùng tài khoản học sinh; từ chối email thuộc tài khoản giáo viên.
- **AC-T00.4** Khi đưa học sinh ra khỏi lớp, học sinh không còn truy cập khóa học của lớp nhưng kết quả học tập đã ghi nhận vẫn được giữ.

### US-T01 — Tạo khóa học
- **AC-T01.1** Giáo viên tạo được khóa học cho lớp mình phụ trách; không được tạo cho lớp của giáo viên khác.
- **AC-T01.2** Học sinh thuộc lớp được xem khóa học; học sinh lớp khác không được xem.

### US-T02A — Tạo và quản lý chủ đề
- **AC-T02A.1** Tạo được nhiều chủ đề trong một khóa học.
- **AC-T02A.2** Chủ đề mới ở trạng thái nháp và học sinh không nhìn thấy.
- **AC-T02A.3** Giáo viên thay đổi được thứ tự hiển thị của các chủ đề.
- **AC-T02A.4** Giáo viên sửa được tiêu đề, mô tả và thực hiện xuất bản hoặc hủy xuất bản chủ đề.
- **AC-T02A.5** Giáo viên chỉ quản lý chủ đề thuộc khóa học mình phụ trách.

### US-T02 — Tạo bài học, mục tiêu học tập và tài liệu
- **AC-T02.1** Bài học mới được tạo ở trạng thái nháp và học sinh không nhìn thấy.
- **AC-T02.2** Không cho phép hai mục tiêu trong cùng bài học có mã trùng nhau.
- **AC-T02.3** Tập tài liệu vượt quá 50 MB hoặc không thuộc định dạng cho phép bị từ chối.
- **AC-T02.4** Bài học phải có ít nhất một mục tiêu học tập mới được xuất bản.
- **AC-T02.5** Hủy xuất bản bài học thì học sinh không còn nhìn thấy bài học, nhưng kết quả học tập đã ghi nhận vẫn được giữ.
- **AC-T02.6** Bài học phải thuộc một chủ đề của cùng khóa học.

### US-T03 — Tạo và xuất bản bài tập
- **AC-T03.1** Bài tập phải có tiêu đề, đề bài, hạn nộp và điểm tối đa hợp lệ.
- **AC-T03.2** Nếu cho phép nộp trễ thì phải thiết lập thời điểm đóng nộp trễ sau hạn nộp.
- **AC-T03.3** Không cho phép xuất bản bài tập có hạn nộp trong quá khứ.
- **AC-T03.4** Bài tập ở trạng thái nháp không hiển thị cho học sinh.
- **AC-T03.5** Tạo được bài tập thuộc một trong ba phạm vi: khóa học, chủ đề hoặc bài học.
- **AC-T03.6** Gắn được nhiều mục tiêu học tập thuộc cùng khóa học; không cho phép gắn mục tiêu trùng lặp hoặc thuộc khóa học khác.

### US-T04 — Chấm bài tập và phản hồi
- **AC-T04.1** Hiển thị danh sách đầy đủ học sinh trong lớp, bao gồm học sinh chưa nộp bài.
- **AC-T04.2** Điểm chấm phải nằm trong khoảng từ 0 đến điểm tối đa của bài tập.
- **AC-T04.3** Kết quả chấm bài được ghi nhận theo các mục tiêu học tập để liên kết với bài tập.
- **AC-T04.4** Khi chấm lại, hệ thống cập nhật kết quả cũ thay vì tạo bản ghi kết quả trùng lặp.

## 6. Nhóm 2 — Quản lý câu hỏi và bài kiểm tra

### US-T05 — Tạo câu hỏi gắn với mục tiêu học tập
- **AC-T05.1** Câu hỏi trắc nghiệm phải có đầy đủ phương án và đáp án đúng mới được lưu.
- **AC-T05.2** Câu hỏi sau khi lưu xuất hiện trong ngân hàng câu hỏi với nguồn "tự soạn".
- **AC-T05.3** Không cho phép gắn câu hỏi với mục tiêu thuộc khóa học khác.
- **AC-T05.4** Câu hỏi nhiều lựa chọn chỉ được điểm khi học sinh chọn đúng và đủ tất cả đáp án đúng.
- **AC-T05.5** Một câu hỏi có thể gắn với nhiều mục tiêu học tập nhưng không được gắn trùng mục tiêu.
- **AC-T05.6** Câu hỏi không gắn mục tiêu vẫn được lưu nhưng không được sử dụng để phân tích kết quả theo mục tiêu.

### US-T06 — Tạo bài kiểm tra từ ngân hàng câu hỏi
- **AC-T06.1** Bài kiểm tra mới ở trạng thái nháp và học sinh không nhìn thấy.
- **AC-T06.2** Thời gian đóng phải sau thời gian mở.
- **AC-T06.3** Không cho phép thêm cùng một câu hỏi nhiều lần trong một bài kiểm tra.
- **AC-T06.4** Tổng điểm tối đa của bài kiểm tra bằng tổng trọng số các câu hỏi.
- **AC-T06.5** Tạo được bài kiểm tra theo phạm vi khóa học, chủ đề hoặc bài học; không được đồng thời chọn chủ đề và bài học.
- **AC-T06.6** Không cho phép chọn chủ đề, bài học hoặc câu hỏi thuộc khóa học khác.

### US-T13 — Xuất bản và hủy xuất bản bài kiểm tra
- **AC-T13.1** Bài kiểm tra ở trạng thái nháp không hiển thị cho học sinh.
- **AC-T13.2** Không cho phép xuất bản bài kiểm tra chưa có câu hỏi.
- **AC-T13.3** Khi xuất bản thành công, học sinh thuộc lớp nhận được thông báo.
- **AC-T13.4** Không cho phép hủy xuất bản bài kiểm tra đã có học sinh làm bài.

### US-T14 — Xem danh sách lượt làm và kết quả bài kiểm tra
- **AC-T14.1** Danh sách hiển thị tất cả học sinh trong lớp, kể cả học sinh chưa làm bài.
- **AC-T14.2** Điểm chính thức của học sinh là điểm cao nhất trong các lượt đã chấm xong.
- **AC-T14.3** Giáo viên lọc được các lượt làm đang chờ chấm và xem chi tiết từng câu trả lời.

### US-T15 — Chấm tay bài kiểm tra
- **AC-T15.1** Điểm từng câu phải nằm trong khoảng từ 0 đến trọng số của câu hỏi.
- **AC-T15.2** Khi tất cả câu hỏi đã được chấm, lượt làm chuyển sang trạng thái "đã chấm" và có tổng điểm.
- **AC-T15.3** Học sinh chỉ nhận thông báo có điểm khi lượt làm đã được chấm hoàn tất.
- **AC-T15.4** Nhận xét của giáo viên cho từng câu được hiển thị cho học sinh.

## 7. Nhóm 3 — Phân tích và hỗ trợ học tập

### US-T07 — Phân tích kết quả học tập theo mục tiêu
- **AC-T07.1** Hiển thị kết quả học tập của từng học sinh theo từng mục tiêu đã được đánh giá.
- **AC-T07.2** Học sinh chưa có kết quả đánh giá ở mục tiêu nào thì hiển thị "Chưa có dữ liệu".
- **AC-T07.3** Hiển thị kết quả trung bình của lớp theo từng mục tiêu, chỉ tính những học sinh đã có dữ liệu.
- **AC-T07.4** Giáo viên xem được các kết quả đánh giá tạo nên kết quả của từng học sinh và truy cập được bài nộp hoặc lượt làm tương ứng.
- **AC-T07.5** Giáo viên chỉ xem được kết quả của các lớp mình phụ trách và có thể lọc theo chủ đề hoặc bài học.

### US-T08 — Xác định nội dung học sinh cần cải thiện
- **AC-T08.1** Hệ thống xác định nội dung cần cải thiện dựa trên kết quả học tập theo từng mục tiêu.
- **AC-T08.2** Nội dung cần cải thiện được xác định khi kết quả đạt mục tiêu dưới ngưỡng quy định và có đủ số lần đánh giá theo cấu hình.
- **AC-T08.3** Khi kết quả học tập được cải thiện đến ngưỡng đạt yêu cầu, nội dung đó không còn được hiển thị trong danh sách cần cải thiện.
- **AC-T08.4** Giáo viên có thể bỏ qua một nội dung cần cải thiện và nhập lý do.
- **AC-T08.5** Nội dung đã bị bỏ qua chỉ được xem xét lại khi có kết quả đánh giá mới.
- **AC-T08.6** Giáo viên có thể lọc nội dung cần cải thiện theo lớp, chủ đề hoặc bài học.

## 8. Nhóm 4 — Ứng dụng AI hỗ trợ giáo viên

### US-T09 — Sử dụng AI tạo câu hỏi nháp
- **AC-T09.1** Khi gửi yêu cầu, hệ thống ghi nhận yêu cầu và hiển thị trạng thái xử lý.
- **AC-T09.2** Số lượng câu hỏi và số lượt yêu cầu AI không được vượt giới hạn cấu hình.
- **AC-T09.3** Câu hỏi có mục tiêu học tập không thuộc bài học được chọn sẽ bị loại.
- **AC-T09.4** Khi xử lý hoàn tất hoặc thất bại, giáo viên nhận được thông báo tương ứng.
- **AC-T09.5** Câu hỏi AI tạo ra ở trạng thái bản nháp và chưa xuất hiện trong ngân hàng câu hỏi.

### US-T10 — Duyệt hoặc chỉnh sửa nội dung do AI tạo
- **AC-T10.1** Từ chối bản nháp thì hệ thống không tạo câu hỏi trong ngân hàng.
- **AC-T10.2** Duyệt bản nháp hợp lệ thì hệ thống tạo câu hỏi và liên kết với mục tiêu học tập tương ứng.
- **AC-T10.3** Chỉnh sửa và lưu bản nháp chưa tạo câu hỏi; chỉ tạo câu hỏi khi giáo viên duyệt.
- **AC-T10.4** Nội dung sau chỉnh sửa phải đáp ứng các yêu cầu hợp lệ của loại câu hỏi tương ứng.
- **AC-T10.5** Giáo viên có thể duyệt hoặc từ chối nhiều bản nháp trong cùng một thao tác.

## 9. Nhóm 5 — Chỉnh sửa và xóa nội dung

### US-T11 — Chỉnh sửa nội dung đã dùng
- **AC-T11.1** Không cho phép đổi mã mục tiêu đã có kết quả đánh giá liên quan; vẫn cho phép sửa mô tả.
- **AC-T11.2** Không cho phép thay đổi điểm tối đa của bài tập đã có bài được chấm.
- **AC-T11.3** Thay đổi hạn nộp không làm thay đổi trạng thái nộp đúng hạn hoặc nộp trễ đã ghi nhận.
- **AC-T11.4** Không cho phép sửa nội dung, loại câu hỏi hoặc mục tiêu của câu hỏi đã được sử dụng trong bài kiểm tra có học sinh làm; vẫn cho phép sửa độ khó và mức Bloom.
- **AC-T11.5** Thay đổi thời lượng bài kiểm tra không làm ảnh hưởng đến thời hạn của lượt làm đang diễn ra.
- **AC-T11.6** Không cho phép đặt số lần làm tối đa thấp hơn số lượt học sinh đã thực hiện.
- **AC-T11.7** Giáo viên sửa được tiêu đề, mô tả và thứ tự hiển thị của chủ đề mà không làm thay đổi kết quả học tập đã ghi nhận.

### US-T12 — Xóa nội dung đã dùng
- **AC-T12.1** Khi xóa bài học đã có kết quả đánh giá, hệ thống hiển thị cảnh báo; sau khi xóa, học sinh không còn nhìn thấy bài học nhưng kết quả liên quan vẫn được giữ.
- **AC-T12.2** Xóa câu hỏi khỏi ngân hàng không làm mất câu hỏi trong các bài kiểm tra đã tạo trước đó.
- **AC-T12.3** Học sinh vẫn xem được kết quả bài tập và bài kiểm tra đã bị xóa.
- **AC-T12.4** Không xóa dữ liệu điểm và kết quả đánh giá đã ghi nhận.
- **AC-T12.5** Khi xóa chủ đề, chủ đề và các bài học bên trong không còn hiển thị cho học sinh nhưng kết quả học tập liên quan vẫn được giữ.

## 10. Nhóm US-S — Học sinh

### US-S01 — Xem bài học đã xuất bản
- **AC-S01.1** Học sinh không nhìn thấy bài học chưa xuất bản.
- **AC-S01.2** Học sinh chỉ truy cập được khóa học thuộc lớp mình.
- **AC-S01.3** Học sinh xem và tải được tài liệu của bài học và tài liệu chung của khóa học.
- **AC-S01.4** Học sinh không xem được chủ đề chưa xuất bản và các bài học bên trong, kể cả khi bài học đã được xuất bản riêng lẻ.

### US-S02 — Làm và nộp bài tập
- **AC-S02.1** Nộp trước hạn được ghi nhận là đúng hạn; nộp trong thời gian cho phép nộp trễ được ghi nhận là nộp trễ.
- **AC-S02.2** Hệ thống từ chối nộp bài khi đã quá thời điểm đóng nộp trễ hoặc bài tập không cho phép nộp trễ.
- **AC-S02.3** Không cho phép nộp bài khi chưa nhập nội dung và chưa đính kèm tệp.
- **AC-S02.4** Sau khi nộp bài, hệ thống chưa tạo kết quả đánh giá cho đến khi giáo viên chấm.
- **AC-S02.5** Bài tập thuộc chủ đề hoặc bài học chưa xuất bản không hiển thị cho học sinh.

### US-S03 — Làm bài kiểm tra trực tuyến
- **AC-S03.1** Không cho phép bắt đầu bài kiểm tra trước thời gian mở, sau thời gian đóng hoặc khi đã hết số lần làm.
- **AC-S03.2** Nếu đang có lượt làm dở, hệ thống mở lại lượt đó thay vì tạo lượt mới.
- **AC-S03.3** Câu trả lời được lưu trong quá trình làm bài; khi hết thời gian, hệ thống tự động nộp các câu trả lời để lưu.
- **AC-S03.4** Câu hỏi trắc nghiệm được chấm tự động; câu trả lời ngắn và tự luận chỉ giáo viên chấm.
- **AC-S03.5** Khi lượt làm được chấm hoàn tất, hệ thống ghi nhận kết quả theo các mục tiêu học tập được liên kết với câu hỏi.
- **AC-S03.6** Điểm của câu hỏi gắn với nhiều mục tiêu được phân bổ tương ứng cho từng mục tiêu.
- **AC-S03.7** Câu hỏi không gắn mục tiêu học tập không tạo kết quả đánh giá theo mục tiêu.

### US-S04 — Xem kết quả và nhận xét
- **AC-S04.1** Bài tập đã chấm hiển thị điểm và nhận xét; bài chưa chấm hiển thị trạng thái "Chờ chấm".
- **AC-S04.2** Bài kiểm tra chỉ hiển thị điểm khi lượt làm đã được chấm hoàn tất.
- **AC-S04.3** Học sinh chỉ xem được kết quả của chính mình.
- **AC-S04.4** Học sinh vẫn xem được kết quả của bài tập hoặc bài kiểm tra đã bị giáo viên xóa.

### US-S05 — Theo dõi kết quả học tập theo mục tiêu
- **AC-S05.1** Hệ thống hiển thị trạng thái "Đã đạt", "Đang tiến bộ" hoặc "Cần cải thiện" dựa trên kết quả đánh giá và tiêu chí đã cấu hình.
- **AC-S05.2** Mỗi mục tiêu hiển thị bài học, chủ đề và các kết quả đánh giá liên quan.
- **AC-S05.3** Mục tiêu chưa có kết quả đánh giá hiển thị "Chưa có dữ liệu".
- **AC-S05.4** Học sinh có thể mở bài học liên quan nếu bài học và chủ đề đang được xuất bản.

### US-S06 — Nhận gợi ý học tập cá nhân hóa
- **AC-S06.1** Học sinh nhận được gợi ý phù hợp với mục tiêu cần cải thiện, đang tiến bộ hoặc đã đạt.
- **AC-S06.2** Mỗi gợi ý có lý do đề xuất và liên kết đến nội dung học tập phù hợp nếu có.
- **AC-S06.3** Học sinh có thể mở nội dung được gợi ý hoặc bỏ qua gợi ý.
- **AC-S06.4** Khi kết quả học tập thay đổi, gợi ý được cập nhật theo kết quả mới.
- **AC-S06.5** Không hiển thị gợi ý trùng lặp cho cùng một học sinh, mục tiêu và nội dung.

### US-S07 — Chỉnh sửa hoặc xóa bài làm trong thời hạn cho phép
- **AC-S07.1** Học sinh được chỉnh sửa bài nộp chưa được chấm khi còn trong thời hạn cho phép.
- **AC-S07.2** Chỉnh sửa sau hạn nộp nhưng trong thời gian nộp trễ làm trạng thái bài nộp chuyển thành nộp trễ.
- **AC-S07.3** Không cho phép chỉnh sửa hoặc xóa bài nộp đã được chấm.
- **AC-S07.4** Học sinh được xóa bài nộp chưa chấm và nộp lại trong thời hạn cho phép; giáo viên nhận thông báo khi có bài nộp lại.
- **AC-S07.5** Khi đã quá thời điểm đóng nộp trễ, học sinh không được chỉnh sửa, xóa hoặc nộp lại bài.

## 11. Nhóm US-H — Hệ thống

### US-H01 — Phân tích và cập nhật kết quả học tập theo mục tiêu
- **AC-H01.1** Sau khi bài tập hoặc bài kiểm tra được chấm, hệ thống cập nhật kết quả theo các mục tiêu học tập có liên quan.
- **AC-H01.2** Khi kết quả đánh giá được chấm lại hoặc cập nhật, hệ thống tính toán lại kết quả tổng hợp và cập nhật gợi ý học tập tương ứng.
- **AC-H01.3** Mục tiêu chưa có dữ liệu đánh giá được hiển thị là "Chưa có dữ liệu", không tự kết luận học sinh chưa đạt.
- **AC-H01.4** Gợi ý không còn phù hợp với kết quả mới được chuyển sang trạng thái hết hiệu lực.
- **AC-H01.5** Nếu xảy ra lỗi phân tích ở một học sinh hoặc mục tiêu, hệ thống ghi nhận lỗi và tiếp tục xử lý các dữ liệu còn lại.
- **AC-H01.6** Kết quả tổng hợp của một mục tiêu bao gồm các kết quả đánh giá được liên kết trực tiếp với mục tiêu đó, không phụ thuộc hoạt động thuộc phạm vi khóa học, chủ đề hay bài học.

## 12. Quy tắc nghiệp vụ xuyên suốt (rút ra từ AC + trigger trong schema)

| # | Quy tắc | Nơi thực thi |
|---|---|---|
| 1 | Điểm bài tập ∈ [0, max_score]; điểm câu ∈ [0, weight] | Trigger `fn_check_submission_score`, `fn_check_answer_score` |
| 2 | Mỗi học sinh chỉ có 1 lượt `IN_PROGRESS` cho mỗi bài kiểm tra | Partial unique index `uq_one_in_progress_attempt` |
| 3 | Không sửa câu hỏi / mục tiêu / điểm tối đa đã bị "khóa" bởi dữ liệu đã chấm | Trigger J.2, J.3 |
| 4 | Sửa/xóa/nộp lại bài nộp chỉ trong cửa sổ nộp (due_date hoặc late_close_date) | `fn_restrict_submission_edit` |
| 5 | Xóa nội dung là **xóa mềm**; điểm và evidence không bị xóa | `deleted_at` + `fn_cleanup_evidence_on_submission_delete` |
| 6 | Evidence chỉ sinh từ mục tiêu thực sự liên kết với hoạt động | `fn_check_evidence_source` |
| 7 | Không mở lại lỗ hổng đã bị bỏ qua nếu chưa có evidence mới hơn | `fn_block_reopen_dismissed_gap` |
| 8 | Mọi trang/kết quả theo lớp phải scope theo `owner_teacher_id` hoặc `class_members` | Tầng ứng dụng |
| 9 | Thông báo sinh tự động ở tầng DB khi có sự kiện | 9 trigger J.4 |
| 10 | Mọi nội dung mới mặc định `DRAFT`, học sinh chỉ thấy `PUBLISHED` | Cột `status` + tầng ứng dụng |
