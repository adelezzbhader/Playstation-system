package playstationsystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import playstationsystem.LogsPage;

public class PlayStationSystem {
    // المسار الصحيح لقاعدة البيانات
    private static final String DB_URL = "jdbc:sqlite:playstation.db";

    private JFrame frame;
    private Map<Integer, JLabel> deviceLabels; // لتخزين الـ Labels الخاصة بكل جهاز
    private Map<Integer, Timer> deviceTimers; // لتخزين الـ Timers الخاصة بكل جهاز
    private Map<Integer, Integer> deviceSessionIds; // لتخزين الـ Session IDs الخاصة بكل جهاز
    private JPanel mainPanel; // اللوحة الرئيسية للأجهزة

    public PlayStationSystem() {
        frame = new JFrame("نظام إدارة أجهزة بلايستيشن");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // جعل النافذة تأخذ حجم الشاشة بالكامل
        frame.setLayout(new BorderLayout());

        deviceLabels = new HashMap<>();
        deviceTimers = new HashMap<>();
        deviceSessionIds = new HashMap<>();

        // إضافة WindowListener لإيقاف التايمرات عند إغلاق الواجهة
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAllTimersAndUpdateDatabase(); // إيقاف التايمرات وتحديث قاعدة البيانات
            }
        });

        // لوحة الأجهزة
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(5, 3, 20, 20)); // 5 صفوف، 3 أعمدة، مع تباعد بين المكونات
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // إضافة هوامش
        mainPanel.setBackground(new Color(245, 245, 245)); // لون خلفية اللوحة الرئيسية

        // إنشاء واجهة لكل جهاز
        for (int i = 1; i <= 15; i++) {
            JPanel devicePanel = new JPanel();
            devicePanel.setLayout(new BorderLayout()); // تنسيق المكونات داخل الجهاز
            devicePanel.setBackground(new Color(255, 255, 255)); // لون خلفية الجهاز
            devicePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20) // إضافة هوامش داخلية
            ));

            JLabel label = new JLabel("الجهاز " + i + ": غير نشط", SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 22)); // تغيير خط النص (حجم 20)
            label.setForeground(new Color(50, 50, 50)); // لون النص

            JLabel timeLabel = new JLabel("", SwingConstants.CENTER); // Label لعرض الوقت المستخدم
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 16)); // تغيير خط النص (حجم 16)
            timeLabel.setForeground(new Color(100, 100, 100)); // لون النص

            JButton startButton = createStyledButton("بدء", new Color(0, 150, 0)); // زر البدء
            JButton endButton = createStyledButton("إيقاف", new Color(150, 0, 0)); // زر الإيقاف
            endButton.setVisible(false); // إخفاء زر الإيقاف في البداية

            int deviceId = i; // رقم الجهاز

            // إضافة ActionListener لزر البدء
            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    startSession(deviceId); // بدء الجلسة للجهاز المحدد
                    startButton.setVisible(false); // إخفاء زر البدء
                    endButton.setVisible(true); // إظهار زر الإيقاف
                }
            });

            // إضافة ActionListener لزر الإيقاف
            endButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    endSession(deviceId); // إنهاء الجلسة للجهاز المحدد
                    endButton.setVisible(false); // إخفاء زر الإيقاف
                    startButton.setVisible(true); // إظهار زر البدء
                }
            });

            // لوحة للأزرار
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
            buttonPanel.setBackground(new Color(255, 255, 255)); // لون خلفية لوحة الأزرار
            buttonPanel.add(startButton);
            buttonPanel.add(endButton);

            // إضافة المكونات إلى لوحة الجهاز
            devicePanel.add(label, BorderLayout.CENTER);
            devicePanel.add(timeLabel, BorderLayout.NORTH); // إضافة Label للوقت المستخدم
            devicePanel.add(buttonPanel, BorderLayout.SOUTH);
            mainPanel.add(devicePanel);

            // حفظ الـ Label في الـ Map
            deviceLabels.put(deviceId, label);
        }

        // وصف النظام
        JLabel descriptionLabel = new JLabel("نظام إدارة أجهزة بلايستيشن - إبدأ الجلسة واضبط الوقت!", SwingConstants.CENTER);
        descriptionLabel.setFont(new Font("Arial", Font.BOLD, 24)); // تغيير خط النص (حجم 24)
        descriptionLabel.setForeground(new Color(0, 120, 215));
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // زر لعرض سجل الأوقات
        JButton viewLogsButton = createStyledButton("عرض السجل", new Color(0, 120, 215));
        viewLogsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LogsPage(); // فتح صفحة السجل
            }
        });

        // لوحة للأزرار
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 245, 245)); // لون خلفية اللوحة
        buttonPanel.add(viewLogsButton);

        // إضافة المكونات إلى النافذة
        frame.add(descriptionLabel, BorderLayout.NORTH);
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        // تحميل الجلسات النشطة من قاعدة البيانات
        loadActiveSessions();
    }

    // دالة لإنشاء أزرار مخصصة
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 18)); // تغيير خط الزر (حجم 18)
        button.setBackground(color); // لون خلفية الزر
        button.setForeground(Color.WHITE); // لون نص الزر
        button.setFocusPainted(false); // إزالة التأثير البصري عند النقر
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // إضافة هوامش للزر
        button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // تغيير شكل المؤشر عند المرور على الزر

        // إضافة تأثير عند النقر
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker()); // تغيير لون الزر عند المرور
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color); // إعادة اللون الأصلي عند الخروج
            }
        });

        return button;
    }

    // بدء جلسة جديدة
    private void startSession(int deviceId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT INTO sessions (device_id, start_time, is_active) VALUES (?, ?, 1)";
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, deviceId);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int sessionId = rs.getInt(1);
                deviceSessionIds.put(deviceId, sessionId); // حفظ الـ Session ID
            }

            deviceLabels.get(deviceId).setText("الجهاز " + deviceId + ": نشط"); // تحديث حالة الجهاز
            startTimer(deviceId); // بدء التوقيت
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // إنهاء الجلسة
    private void endSession(int deviceId) {
        if (!deviceSessionIds.containsKey(deviceId)) {
            return;
        }

        int sessionId = deviceSessionIds.get(deviceId);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // جلب وقت البداية
            String sql = "SELECT start_time FROM sessions WHERE session_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, sessionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = LocalDateTime.now();
                Duration duration = Duration.between(startTime, endTime);

                // تحويل الوقت إلى ساعات ودقائق وثواني
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                long seconds = duration.getSeconds() % 60;

                // تحديث الجلسة بوقت النهاية والمدة
                sql = "UPDATE sessions SET end_time = ?, duration = ?, is_active = 0 WHERE session_id = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setTimestamp(1, Timestamp.valueOf(endTime));
                pstmt.setInt(2, (int) duration.getSeconds()); // حفظ المدة بالثواني
                pstmt.setInt(3, sessionId);
                pstmt.executeUpdate();

                deviceLabels.get(deviceId).setText("الجهاز " + deviceId + ": غير نشط");
                deviceLabels.get(deviceId).setText("<html>الجهاز " + deviceId + ": غير نشط<br>الوقت المستخدم: " +
                        hours + "س " + minutes + "د " + seconds + "ث</html>");
                deviceSessionIds.remove(deviceId); // إزالة الـ Session ID
                stopTimer(deviceId); // إيقاف التوقيت
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // بدء التوقيت للجهاز
    private void startTimer(int deviceId) {
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTimer(deviceId); // تحديث التوقيت كل ثانية
            }
        });
        timer.start();
        deviceTimers.put(deviceId, timer); // حفظ الـ Timer
    }

    // إيقاف التوقيت للجهاز
    private void stopTimer(int deviceId) {
        Timer timer = deviceTimers.get(deviceId);
        if (timer != null) {
            timer.stop();
            deviceTimers.remove(deviceId); // إزالة الـ Timer
        }
    }

    // تحديث التوقيت للجهاز
    private void updateTimer(int deviceId) {
        if (!deviceSessionIds.containsKey(deviceId)) {
            return;
        }

        int sessionId = deviceSessionIds.get(deviceId);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT start_time FROM sessions WHERE session_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, sessionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                Duration duration = Duration.between(startTime, LocalDateTime.now());

                // تحويل الوقت إلى ساعات ودقائق وثواني
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                long seconds = duration.getSeconds() % 60;

                deviceLabels.get(deviceId).setText("الجهاز " + deviceId + ": نشط (" +
                        hours + "س " + minutes + "د " + seconds + "ث)");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // تحميل الجلسات النشطة من قاعدة البيانات
    private void loadActiveSessions() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT session_id, device_id, start_time FROM sessions WHERE is_active = 1";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int sessionId = rs.getInt("session_id");
                int deviceId = rs.getInt("device_id");
                deviceSessionIds.put(deviceId, sessionId);
                deviceLabels.get(deviceId).setText("الجهاز " + deviceId + ": نشط");
                startTimer(deviceId);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // إيقاف جميع التايمرات وتحديث قاعدة البيانات عند إغلاق الواجهة
    private void stopAllTimersAndUpdateDatabase() {
        for (Map.Entry<Integer, Timer> entry : deviceTimers.entrySet()) {
            int deviceId = entry.getKey();
            Timer timer = entry.getValue();
            timer.stop(); // إيقاف التايمر
            endSession(deviceId); // إنهاء الجلسة وتحديث قاعدة البيانات
        }
    }

    public static void main(String[] args) {
        new PlayStationSystem();
    }
}