
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

public class GameOfLife extends JFrame {
    private final int WIDTH = 1400;
    private final int HEIGHT = 800;
    private int cellSize = 10;
    private int rows;
    private int cols;
    
    private boolean[][] grid;
    private boolean[][] nextGrid;
    private boolean isRunning = false;
    private int delay = 100; // milliseconds between updates
    private int updateCount = 0;
    private boolean showGridLines = true;
    
    public GameOfLife() {
        setTitle("Conway's Game of Life");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Initialize grid dimensions
        updateGridDimensions();
        
        // Main control panel
        JPanel mainControlPanel = new JPanel();
        mainControlPanel.setLayout(new BorderLayout());
        
        // Top row of controls
        JPanel topControls = new JPanel();
        JButton startPauseButton = new JButton("Start");
        JButton clearButton = new JButton("Clear");
        JButton randomButton = new JButton("Random");
        
        // Speed control panel
        JPanel speedPanel = new JPanel();
        speedPanel.setBorder(BorderFactory.createTitledBorder("Speed Control"));
        
        // Add delay input field with label
        JLabel delayLabel = new JLabel("Delay (ms):");
        JTextField delayField = new JTextField(5);
        delayField.setText("100");
        speedPanel.add(delayLabel);
        speedPanel.add(delayField);
        
        // Grid settings panel
        JPanel gridPanel = new JPanel();
        gridPanel.setBorder(BorderFactory.createTitledBorder("Grid Settings"));
        
        // Cell size control
        JPanel cellSizePanel = new JPanel();
        JLabel cellSizeLabel = new JLabel("Cell Size:");
        JSpinner cellSizeSpinner = new JSpinner(new SpinnerNumberModel(cellSize, 2, 50, 1));
        cellSizePanel.add(cellSizeLabel);
        cellSizePanel.add(cellSizeSpinner);
        
        // Show grid lines checkbox
        JCheckBox showGridLinesCheckBox = new JCheckBox("Show Grid Lines", showGridLines);
        
        gridPanel.add(cellSizePanel);
        gridPanel.add(showGridLinesCheckBox);
        
        // Add buttons to top controls
        topControls.add(startPauseButton);
        topControls.add(clearButton);
        topControls.add(randomButton);
        
        // Add panels to main control panel
        mainControlPanel.add(topControls, BorderLayout.NORTH);
        JPanel bottomControls = new JPanel(new GridLayout(1, 2));
        bottomControls.add(speedPanel);
        bottomControls.add(gridPanel);
        mainControlPanel.add(bottomControls, BorderLayout.CENTER);
        
        // Add control panel to the bottom of the frame
        add(mainControlPanel, BorderLayout.SOUTH);
        
        // Create the game panel
        GamePanel gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);
        
        // Add mouse listener to toggle cells
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = e.getY() / cellSize;
                int col = e.getX() / cellSize;
                
                if (row >= 0 && row < rows && col >= 0 && col < cols) {
                    grid[row][col] = !grid[row][col];
                    gamePanel.repaint();
                }
            }
        });
        
        // Thread reference to control the simulation
        final Thread[] simulationThread = new Thread[1];
        
        // Button actions
        startPauseButton.addActionListener(e -> {
            if (!isRunning) {
                // Start the simulation
                isRunning = true;
                startPauseButton.setText("Pause");
                
                // Update delay from input field
                try {
                    int newDelay = Integer.parseInt(delayField.getText());
                    if (newDelay >= 0) {
                        delay = newDelay;
                    }
                } catch (NumberFormatException ex) {
                    // If invalid input, keep current delay
                }
                
                simulationThread[0] = new Thread(() -> {
                    while (isRunning) {
                        updateGrid();
                        updateCount++;
                        gamePanel.repaint();
                        
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                simulationThread[0].start();
            } else {
                // Pause the simulation
                isRunning = false;
                startPauseButton.setText("Start");
            }
        });
        
        clearButton.addActionListener(e -> {
            isRunning = false;
            startPauseButton.setText("Start");
            clearGrid();
            gamePanel.repaint();
        });
        
        randomButton.addActionListener(e -> {
            isRunning = false;
            startPauseButton.setText("Start");
            randomizeGrid();
            gamePanel.repaint();
        });
        
        // Cell size spinner action
        cellSizeSpinner.addChangeListener(e -> {
            int newCellSize = (Integer) cellSizeSpinner.getValue();
            if (newCellSize != cellSize) {
                boolean[][] oldGrid = grid;
                int oldRows = rows;
                int oldCols = cols;
                
                // Save simulation state
                boolean wasRunning = isRunning;
                isRunning = false;
                
                // Update cell size and grid dimensions
                cellSize = newCellSize;
                updateGridDimensions();
                
                // Try to preserve existing pattern (copy old grid to new grid)
                clearGrid();
                int minRows = Math.min(oldRows, rows);
                int minCols = Math.min(oldCols, cols);
                for (int row = 0; row < minRows; row++) {
                    for (int col = 0; col < minCols; col++) {
                        grid[row][col] = oldGrid[row][col];
                    }
                }
                
                // Restart simulation if it was running
                if (wasRunning) {
                    startPauseButton.doClick();
                } else {
                    gamePanel.repaint();
                }
            }
        });
        
        // Grid lines checkbox action
        showGridLinesCheckBox.addActionListener(e -> {
            showGridLines = showGridLinesCheckBox.isSelected();
            gamePanel.repaint();
        });
        
        // Update delay when input field changes
        delayField.addActionListener(e -> {
            try {
                int newDelay = Integer.parseInt(delayField.getText());
                if (newDelay >= 0) {
                    delay = newDelay;
                }
            } catch (NumberFormatException ex) {
                // Reset to current value if invalid
                delayField.setText(String.valueOf(delay));
            }
        });
        
        setVisible(true);
    }
    
    private void updateGridDimensions() {
        rows = HEIGHT / cellSize;
        cols = WIDTH / cellSize;
        
        // Initialize the grids with new dimensions
        grid = new boolean[rows][cols];
        nextGrid = new boolean[rows][cols];
    }
    
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Draw cells first (background layer)
            g.setColor(Color.BLACK);
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    if (grid[row][col]) {
                        if (showGridLines) {
                            // Slightly smaller cells when grid lines are shown
                            g.fillRect(col * cellSize + 1, row * cellSize + 1, 
                                    cellSize - 1, cellSize - 1);
                        } else {
                            // Full cells when grid lines are hidden
                            g.fillRect(col * cellSize, row * cellSize, 
                                    cellSize, cellSize);
                        }
                    }
                }
            }
            
            // Draw grid lines if enabled (foreground layer)
            if (showGridLines) {
                g.setColor(Color.LIGHT_GRAY);
                // Draw horizontal lines
                for (int i = 0; i <= rows; i++) {
                    g.drawLine(0, i * cellSize, cols * cellSize, i * cellSize);
                }
                // Draw vertical lines
                for (int i = 0; i <= cols; i++) {
                    g.drawLine(i * cellSize, 0, i * cellSize, rows * cellSize);
                }
            }
        }
    }
    
    private void updateGrid() {
        // Apply Game of Life rules
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int neighbors = countNeighbors(row, col);
                
                // Apply rules
                if (grid[row][col]) {
                    // Cell is alive
                    nextGrid[row][col] = neighbors == 2 || neighbors == 3;
                } else {
                    // Cell is dead
                    nextGrid[row][col] = neighbors == 3;
                }
            }
        }
        
        // Swap grids
        boolean[][] temp = grid;
        grid = nextGrid;
        nextGrid = temp;
    }
    
    private int countNeighbors(int row, int col) {
        int count = 0;
        
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue; // Skip the cell itself
                
                int r = row + i;
                int c = col + j;
                
                // Check bounds and count living neighbors
                if (r >= 0 && r < rows && c >= 0 && c < cols && grid[r][c]) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private void clearGrid() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                grid[row][col] = false;
                nextGrid[row][col] = false;
            }
        }
    }
    
    private void randomizeGrid() {
        Random random = new Random();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                grid[row][col] = random.nextDouble() < 0.2; // 20% chance of a cell being alive
                nextGrid[row][col] = false;
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameOfLife::new);
    }
}