import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;

public class weather extends JFrame{

    private JTextField locationField;
    private JComboBox<String> infoComboBox;
    private JComboBox<String> unitComboBox;
    private JTextArea weatherTextArea;
    private JLabel timeLabel;
    private String location;
    private String temperature;
    private String pressure;
    private String windSpeed;
    private String humidity;

    private List<String> history = new ArrayList<>();

    public weather() {
        setTitle("Weather Forecast App");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0x3498db)); // Set background color

        // Create components
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(7, 1, 10, 10)); // Increased the rows to accommodate "History"
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(0x3498db)); // Set background color

        JLabel locationLabel = new JLabel("Enter Location:");
        locationField = new JTextField(20);

        JLabel infoLabel = new JLabel("Select Info:");
        String[] infoOptions = { "Temperature", "Humidity", "Wind Speed", "Pressure", "Precipitation" };
        infoComboBox = new JComboBox<>(infoOptions);

        JLabel unitLabel = new JLabel("Select Unit:");
        String[] unitOptions = { "Celsius", "Fahrenheit", "Meter/second", "Kilometer/hour" };
        unitComboBox = new JComboBox<>(unitOptions);

        JButton getWeatherButton = new JButton("Get Weather Info");
        getWeatherButton.setBackground(new Color(0x4CAF50));
        getWeatherButton.setForeground(Color.WHITE);
        getWeatherButton.setFocusPainted(false);

        JButton historyButton = new JButton("History");
        historyButton.setBackground(new Color(0xFF5733));
        historyButton.setForeground(Color.WHITE);
        historyButton.setFocusPainted(false);

        timeLabel = new JLabel();
        updateTime(); // Update time immediately
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTime(); // Update time every second
            }
        });
        timer.start();

        weatherTextArea = new JTextArea(10, 30);
        weatherTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(weatherTextArea);

        // Add components to the panel in the desired order
        panel.add(locationLabel);
        panel.add(locationField);
        panel.add(infoLabel);
        panel.add(infoComboBox);
        panel.add(unitLabel);
        panel.add(unitComboBox);
        panel.add(getWeatherButton);
        panel.add(historyButton);
        panel.add(timeLabel);

        // Add the panel to the frame
        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        getWeatherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String location = locationField.getText().trim();
                String selectedInfo = (String) infoComboBox.getSelectedItem();
                String selectedUnit = (String) unitComboBox.getSelectedItem();

                if (!location.isEmpty()) {
                    String apiKey = "16ca8ac87029671d0bb435e0cafdfeb0";
                    String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + location + "&appid="
                            + apiKey;
                    String weatherData = getWeatherData(apiUrl, selectedInfo.toLowerCase(), selectedUnit);
                    switch (selectedInfo.toLowerCase()) {
                        case "temperature":
                            temperature = weatherData;
                            break;
                        case "humidity":
                            humidity = weatherData;
                            break;
                        case "wind speed":
                            windSpeed = weatherData;
                            break;
                        case "pressure":
                            pressure = weatherData;
                            break;
                    }
                    weatherTextArea.setText(weatherData);

                    // Add the search entry to the history
                    history.add("Location: " + location + ", Info: " + selectedInfo + ", Unit: " + selectedUnit);
                    boolean insertSuccess = DatabaseHandler.insertWeatherData(location, temperature, pressure,
                            windSpeed, humidity);
                    if (!insertSuccess) {
                        weatherTextArea.append("\nError inserting data into the database.");
                    }
                } else {
                    weatherTextArea.setText("Please enter a valid location.");
                }
            }

        });

        historyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new HistoryPage(history).setVisible(true);
            }
        });

        infoComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedInfo = (String) infoComboBox.getSelectedItem();
                // Disable the unitComboBox for certain info types
                boolean disableUnitComboBox = selectedInfo.equals("Humidity") ||
                        selectedInfo.equals("Pressure") ||
                        selectedInfo.equals("Precipitation");
                unitComboBox.setEnabled(!disableUnitComboBox);

                // Update unitComboBox options based on selected info
                if (selectedInfo.equals("Temperature")) {
                    unitComboBox.removeAllItems();
                    unitComboBox.addItem("Celsius");
                    unitComboBox.addItem("Fahrenheit");
                } else if (selectedInfo.equals("Wind Speed")) {
                    unitComboBox.removeAllItems();
                    unitComboBox.addItem("Meter/second");
                    unitComboBox.addItem("Kilometer/hour");
                } else {
                    unitComboBox.removeAllItems();
                    unitComboBox.addItem("N/A");
                }
            }
        });

    }

    private String getWeatherData(String apiUrl, String info, String unit) {
        // Same as your previous code for fetching weather data

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n"); // Append new line for each response line
            }

            switch (info) {
                case "temperature":
                    return extractTemperature(response, unit);
                case "humidity":
                    return extractData(response, "humidity");
                case "wind speed":
                    return extractData(response, "speed") + " " + unit;
                case "pressure":
                    return extractData(response, "pressure") + " " + unit;
                case "precipitation":
                    return extractData(response, "precipitation");
                default:
                    return "Invalid info selection.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching weather data.";
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String extractTemperature(StringBuilder response, String unit) {
        String searchKey = "\"temp\":";
        int index = response.indexOf(searchKey);
        if (index != -1) {
            int startIndex = index + searchKey.length();
            int endIndex = response.indexOf(",", startIndex);
            double temperatureInKelvin = Double.parseDouble(response.substring(startIndex, endIndex).trim());

            if (unit.equals("Celsius")) {
                double temperatureInCelsius = temperatureInKelvin - 273.15;
                return "Temperature: " + temperatureInCelsius + "°C";
            } else {
                double temperatureInFahrenheit = (temperatureInKelvin - 273.15) * 9 / 5 + 32;
                return "Temperature: " + temperatureInFahrenheit + "°F";
            }
        } else {
            return "Temperature data not available.";
        }
    }

    private String extractData(StringBuilder response, String key) {
        String searchKey = "\"" + key + "\":";
        int index = response.indexOf(searchKey);
        if (index != -1) {
            int startIndex = index + searchKey.length();
            int endIndex = response.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = response.indexOf("}", startIndex);
            }
            String data = response.substring(startIndex, endIndex).trim();
            try {
                if (key.equals("humidity")) {
                    return "Humidity: " + data + "%";
                } else if (key.equals("pressure")) {
                    return "Pressure: " + data + " hPa";
                } else {
                    double value = Double.parseDouble(data);
                    if (key.equals("speed")) {
                        // Convert wind speed to selected unit
                        String selectedUnit = (String) unitComboBox.getSelectedItem();
                        if (selectedUnit.equals("Kilometer/hour")) {
                            value *= 3.6; // 1 m/s = 3.6 km/h
                        } else if (selectedUnit.equals("Miles/hour")) {
                            value *= 2.23694; // 1 m/s = 2.23694 mph
                        }
                    }
                    return key.substring(0, 1).toUpperCase() + key.substring(1) + ": " + value;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return "Error parsing wind speed data.";
            }
        } else {
            return "Data not available.";
        }
    }

    private void updateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String formattedDate = dateFormat.format(now);
        timeLabel.setText("Current Date and Time: " + formattedDate);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new weather().setVisible(true);
            }
        });
    }
}

class HistoryPage extends JFrame {

    private JTextArea historyTextArea;
    private Timer animationTimer;
    private int animationCounter = 0;

    public HistoryPage(List<String> history) {
        setTitle("Search History");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0x3498db)); // Set blue background color

        historyTextArea = new JTextArea(10, 30); // Make it an instance variable
        historyTextArea.setEditable(false);
        historyTextArea.setBackground(new Color(0x3498db)); // Set blue text area background color
        historyTextArea.setForeground(Color.BLACK); // Set black text color
        historyTextArea.setFont(new Font("Arial", Font.PLAIN, 14)); // Set custom font
        historyTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        JScrollPane scrollPane = new JScrollPane(historyTextArea);

        StringBuilder historyText = new StringBuilder("Search History:\n");
        for (String entry : history) {
            historyText.append(entry).append("\n");
        }
        historyTextArea.setText(historyText.toString());

        add(scrollPane, BorderLayout.CENTER);

        // Set up animation timer
        animationTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animateText();
            }
        });
        animationTimer.start();
    }

    public class DatabaseHandler {

        private static final String JDBC_URL = "jdbc:mysql://localhost:3306/weatherapp";
        private static final String USERNAME = "root";
        private static final String PASSWORD = "manager";

        public static boolean insertWeatherData(String location, String temperature, String pressure, String windSpeed,
                String humidity) {
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);

                // Create the weather table if it does not exist
                String createTableQuery = "CREATE TABLE IF NOT EXISTS weather (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "location VARCHAR(255)," +
                        "temperature VARCHAR(255)," +
                        "pressure VARCHAR(255)," +
                        "wind_speed VARCHAR(255)," +
                        "humidity VARCHAR(255))";

                try (PreparedStatement createTableStatement = connection.prepareStatement(createTableQuery)) {
                    createTableStatement.executeUpdate();
                }

                // Prepare the SQL INSERT query
                String insertQuery = "INSERT INTO weather (location, temperature, pressure, wind_speed, humidity) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                    // Set the values in the prepared statement
                    preparedStatement.setString(1, location);
                    preparedStatement.setString(2, temperature);
                    preparedStatement.setString(3, pressure);
                    preparedStatement.setString(4, windSpeed);
                    preparedStatement.setString(5, humidity);

                    // Execute the INSERT query
                    preparedStatement.executeUpdate();

                    // Data insertion successful, print message and return true
                    System.out.println("Data inserted into the database successfully!");
                    return true;
                }
            } catch (SQLException e) {
                // Handle any database errors and print the error details
                e.printStackTrace();
                System.err.println("Error inserting data into the database: " + e.getMessage());
                return false;
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }

    private void animateText() {
        // Animation logic
        int red = animationCounter % 256;
        int green = animationCounter % 256;
        int blue = animationCounter % 256;

        historyTextArea.setForeground(new Color(red, green, blue));

        // Increment the animation counter
        animationCounter += 10;
        if (animationCounter > 255) {
            animationCounter = 0;
        }
    }

}
