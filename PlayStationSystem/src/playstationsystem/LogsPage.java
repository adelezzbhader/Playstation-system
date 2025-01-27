package playstationsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import javax.swing.table.JTableHeader;

public class LogsPage extends JFrame {
    // المسار الصحيح لقاعدة البيانات
    private static final String DB_URL = "jdbc:sqlite:playstation.db";

    public LogsPage() {
        setTitle("سجل الأوقات");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // جعل النافذة تأخذ حجم الشاشة بالكامل
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // إغلاق النافذة فقط عند الضغط على X

        // لوحة رئيسية للواجهة
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // إضافة هوامش
        mainPanel.setBackground(new Color(245, 245, 245)); // لون خلفية اللوحة الرئيسية

        // إنشاء جدول لعرض البيانات
        String[] columns = {"رقم الجهاز", "وقت البدء", "وقت الانتهاء", "المدة (ساعات:دقائق:ثواني)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 16)); // تغيير خط الجدول
        table.setRowHeight(30); // تحديد ارتفاع الصفوف
        table.setSelectionBackground(new Color(0, 120, 215)); // لون خلفية الصف المحدد
        table.setSelectionForeground(Color.WHITE); // لون نص الصف المحدد
        table.setGridColor(new Color(200, 200, 200)); // لون خطوط الجدول

        // تحسين تنسيق رأس الجدول
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 18)); // تغيير خط رأس الجدول
        header.setBackground(new Color(0, 120, 215)); // لون خلفية رأس الجدول
        header.setForeground(Color.WHITE); // لون نص رأس الجدول

        // جلب البيانات من قاعدة البيانات
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT device_id, start_time, end_time, duration FROM sessions WHERE strftime('%Y-%m-%d', datetime(start_time / 1000, 'unixepoch')) = strftime('%Y-%m-%d', 'now')";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int deviceId = rs.getInt("device_id");
                Timestamp startTime = rs.getTimestamp("start_time");
                Timestamp endTime = rs.getTimestamp("end_time");
                int duration = rs.getInt("duration");

                // تحويل المدة إلى تنسيق ساعات:دقائق:ثواني
                String formattedDuration = formatDuration(duration);

                // إضافة البيانات إلى الجدول
                model.addRow(new Object[]{deviceId, startTime, endTime, formattedDuration});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // إضافة الجدول إلى JScrollPane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // إضافة هوامش
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // إضافة إجمالي الوقت لكل جهاز في كاردات
        JPanel cardsPanel = new JPanel(new GridLayout(0, 3, 20, 20)); // 3 أعمدة، عدد الصفوف يتغير تلقائيًا
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // إضافة هوامش
        cardsPanel.setBackground(new Color(245, 245, 245)); // لون خلفية اللوحة

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            for (int i = 1; i <= 15; i++) {
                // حساب إجمالي وقت اللعب لكل جهاز في اليوم الحالي
                String sql = "SELECT SUM(duration) AS total FROM sessions WHERE device_id = ? AND strftime('%Y-%m-%d', datetime(start_time / 1000, 'unixepoch')) = strftime('%Y-%m-%d', 'now')";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, i);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int totalDuration = rs.getInt("total");
                    String formattedTime = formatDuration(totalDuration); // تحويل الوقت إلى ساعات ودقائق وثواني

                    // إنشاء كارد لكل جهاز
                    JPanel card = createStyledCard("الجهاز " + i, formattedTime);
                    cardsPanel.add(card);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // إضافة عنوان للجزء السفلي
        JLabel totalTitle = new JLabel("إجمالي وقت اللعب لكل جهاز (اليوم الحالي):", SwingConstants.CENTER);
        totalTitle.setFont(new Font("Arial", Font.BOLD, 20)); // تغيير الخط
        totalTitle.setForeground(new Color(0, 120, 215)); // لون النص
        totalTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // إضافة هوامش

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(245, 245, 245)); // لون خلفية اللوحة
        bottomPanel.add(totalTitle, BorderLayout.NORTH);
        bottomPanel.add(cardsPanel, BorderLayout.CENTER);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    // دالة لإنشاء كارد مخصص
    private JPanel createStyledCard(String title, String time) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE); // لون خلفية الكارد
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20) // إضافة هوامش داخلية
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18)); // تغيير الخط
        titleLabel.setForeground(new Color(0, 120, 215)); // لون النص

        JLabel timeLabel = new JLabel(time, SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 16)); // تغيير الخط
        timeLabel.setForeground(new Color(50, 50, 50)); // لون النص

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(timeLabel, BorderLayout.CENTER);

        return card;
    }

    // دالة لتحويل الوقت من الثواني إلى ساعات ودقائق وثواني
    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds); // تنسيق الوقت
    }
}