package Common;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String name;
    private List<String> bidHistory;

    public User(int id, String name) {
        this.id = id;
        this.name = name;
        this.bidHistory = new ArrayList<>();
    }

    // ===================== GETTER =====================
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getBidHistory() {
        return bidHistory;
    }

    // ===================== ADD HISTORY =====================
    public void addHistory(String record) {
        bidHistory.add(record);
    }

    // ===================== FORMAT HISTORY =====================
    public String getHistoryAsString() {
        if (bidHistory.isEmpty()) return "Chưa có lịch sử.";

        StringBuilder sb = new StringBuilder();
        sb.append("Lịch sử của ").append(name).append(":\n");

        for (String s : bidHistory) {
            sb.append("- ").append(s).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "User[" + id + "] - " + name;
    }
}