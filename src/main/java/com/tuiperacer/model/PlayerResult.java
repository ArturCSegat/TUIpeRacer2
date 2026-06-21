package com.tuiperacer.model;

public class PlayerResult {

    private String playerName;
    private PlayerType playerType;
    private double wpm;
    private double accuracy;
    private boolean finished;
    private String typedText;

    public PlayerResult() {}

    public PlayerResult(String playerName, PlayerType playerType, double wpm,
                        double accuracy, boolean finished, String typedText) {
        this.playerName = playerName;
        this.playerType = playerType;
        this.wpm = wpm;
        this.accuracy = accuracy;
        this.finished = finished;
        this.typedText = typedText;
    }

    public static PlayerResult fromPlayer(Player player) {
        return new PlayerResult(
                player.getName(),
                player.getType(),
                player.getWPM(),
                player.getAccuracy(),
                player.isFinished(),
                player.getTypedText()
        );
    }

    public String serialize() {
        return playerName + "|" + playerType.name() + "|" +
               String.format("%.2f", wpm) + "|" +
               String.format("%.2f", accuracy) + "|" +
               finished + "|" +
               typedText.replace("|", "\\|").replace("\n", "\\n");
    }

    public static PlayerResult deserialize(String data) {
        String[] parts = data.split("\\|(?<!\\\\\\|)", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid PlayerResult data: " + data);
        }
        PlayerResult result = new PlayerResult();
        result.playerName = parts[0];
        try {
            result.playerType = PlayerType.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            result.playerType = PlayerType.LOCAL;
        }
        try {
            result.wpm = Double.parseDouble(parts[2]);
            result.accuracy = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            result.wpm = 0.0;
            result.accuracy = 0.0;
        }
        result.finished = Boolean.parseBoolean(parts[4]);
        result.typedText = parts[5].replace("\\|", "|").replace("\\n", "\n");
        return result;
    }

    // Getters and setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public PlayerType getPlayerType() { return playerType; }
    public void setPlayerType(PlayerType playerType) { this.playerType = playerType; }

    public double getWpm() { return wpm; }
    public void setWpm(double wpm) { this.wpm = wpm; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }

    public String getTypedText() { return typedText; }
    public void setTypedText(String typedText) { this.typedText = typedText; }
}
