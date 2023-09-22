import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

public class BrickBreakerGame extends JPanel implements ActionListener, KeyListener {

    boolean Q_LearningStatus = false;

    private Timer timer;
    private int ballX = 150;
    private int ballY = 300;
    private int ballDirectionX = 3;
    private int ballDirectionY = 3;
    private int paddleX = 250;
    private int paddleDirectionX = 0;
    private boolean[][] bricks;
    private final int brickWidth = 50;
    private final int brickHeight = 20;
    private final int brickGap = 5;
    private final int numBricksX = 11;
    private final int numBricksY = 7;
    private int score = 0;
    private Color[][] brickColors;
    private boolean gameOver = false;

    // 추가된 변수들
    private int playerHealth = 5; // 플레이어 체력
    private int[] attackX; // 공격의 X 좌표 배열
    private int[] attackY; // 공격의 Y 좌표 배열
    private boolean[] attackActive; // 공격 활성화 여부 배열
    private final int attackSpeed = 2; // 공격 이동 속도
    private int[] attackDirectionX; // 공격 방향 X 좌표 배열
    private int[] attackDirectionY; // 공격 방향 Y 좌표 배열

    private int[] healthPackX; // 공격의 X 좌표 배열
    private int[] healthPackY; // 공격의 Y 좌표 배열
    private boolean[] healthPackActive; // 공격 활성화 여부 배열
    private int healthPackSpeed = 2; // 공격 이동 속도
    private int[] healthPackDirectionX; // 공격 방향 X 좌표 배열
    private int[] healthPackDirectionY; // 공격 방향 Y 좌표 배열


    // Q Learning
    private double[][] qTable;  // Q-테이블 (상태-행동 쌍에 대한 Q-값 저장)
    private int currentState;   // 현재 상태 (게임의 상태를 숫자로 표현)

    private final double learningRate = 0.1;  // 학습률
    private final double discountFactor = 0.9;  // 할인 계수
    private final double explorationRate = 0.2;  // 탐험 비율 (탐욕적 선택을 위한 확률)

    private final int numActions = 2;

    /**
     * Q Learning
     */
    // Q-값 업데이트 함수 (Q-Learning 알고리즘)
    private void updateQValue(int state, int action, double reward, int nextState) {
        double maxQ = getMaxQValue(nextState);
        double currentQ = qTable[state][action];
        double newQ = currentQ + learningRate * (reward + discountFactor * maxQ - currentQ);
        qTable[state][action] = newQ;
    }

    // 탐욕적인 행동 선택 함수
    private int selectGreedyAction(int state) {
        int bestAction = 0;
        double bestQValue = qTable[state][0];

        for (int action = 1; action < numActions; action++) {
            double qValue = qTable[state][action];
            if (qValue > bestQValue) {
                bestQValue = qValue;
                bestAction = action;
            }
        }

        return bestAction;
    }

    // 무작위 행동 선택 함수 (탐험)
    private int selectRandomAction() {
        return (int) (Math.random() * numActions);
    }

    // Q-값 중 최대값을 찾는 함수
    private double getMaxQValue(int state) {
        double maxQ = qTable[state][0];
        for (int action = 1; action < numActions; action++) {
            double qValue = qTable[state][action];
            if (qValue > maxQ) {
                maxQ = qValue;
            }
        }
        return maxQ;
    }



    public BrickBreakerGame() {

        // 게임 시작 메뉴
        String[] options = {"게임 시작", "강화학습 시키기", "게임 종료"};
        int choice = JOptionPane.showOptionDialog(null, "원하는 옵션을 선택하세요:", "게임 메뉴", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) { // "게임 시작"을 선택한 경우
            Q_LearningStatus = false;
            startGame();
        } else if (choice == 1) { // "강화학습 시키기"를 선택한 경우
            Q_LearningStatus = true;
            startGame(); // 강화학습 코드를 실행
        } else { // "게임 종료"를 선택한 경우
            System.exit(0);
        }

    }

    private void startGame() {

        bricks = new boolean[numBricksX][numBricksY];
        brickColors = new Color[numBricksX][numBricksY]; // 벽돌 색상 배열 초기화

        // 벽돌 색상 초기화
        Random rand = new Random();
        for (int i = 0; i < numBricksX; i++) {
            for (int j = 0; j < numBricksY; j++) {
                bricks[i][j] = true;
                brickColors[i][j] = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            }
        }
        timer = new Timer(5, this);
        timer.start();
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        // 공격 초기화
        attackX = new int[numBricksX];
        attackY = new int[numBricksX];
        attackActive = new boolean[numBricksX];
        attackDirectionX = new int[numBricksX];
        attackDirectionY = new int[numBricksX];
        for (int i = 0; i < numBricksX; i++) {
            attackX[i] = i * (brickWidth + brickGap) + brickWidth / 2;
            attackY[i] = 0;
            attackActive[i] = false;
            attackDirectionX[i] = 0; // 초기값 0
            attackDirectionY[i] = 0; // 초기값 0
        }

        // HealthPack 초기화
        healthPackX = new int[numBricksX];
        healthPackY = new int[numBricksX];
        healthPackActive = new boolean[numBricksX];
        healthPackDirectionX = new int[numBricksX];
        healthPackDirectionY = new int[numBricksX];
        for (int i = 0; i < numBricksX; i++) {
            healthPackX[i] = i * (brickWidth + brickGap) + brickWidth / 2;
            healthPackY[i] = 0;
            healthPackActive[i] = false;
            healthPackDirectionX[i] = 0; // 초기값 0
            healthPackDirectionY[i] = 0; // 초기값 0
        }

        // 게임 루프 시작
        Timer timer = new Timer(10, this);
        timer.start();
    }

    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            ballX += ballDirectionX;
            ballY += ballDirectionY;

            // 벽과의 충돌 감지
            if (ballX <= 0 || ballX >= getWidth() - 20) {
                ballDirectionX = -ballDirectionX;
            }
            if (ballY <= 0) {
                ballDirectionY = -ballDirectionY;
            }
            if (ballY >= getHeight() - 20) {
                // 공이 바닥에 닿으면 게임 오버 처리
                gameOver = true;
                showGameOverDialog();
            }

            // 패들과의 충돌 감지
            if (ballY >= getHeight() - 50 && ballX >= paddleX && ballX <= paddleX + 100) {
                ballDirectionY = -ballDirectionY;
            }

            // 벽돌과의 충돌 감지
            for (int i = 0; i < numBricksX; i++) {
                for (int j = 0; j < numBricksY; j++) {
                    if (bricks[i][j]) {
                        int brickX = i * (brickWidth + brickGap);
                        int brickY = j * (brickHeight + brickGap);
                        if (ballX >= brickX && ballX <= brickX + brickWidth && ballY >= brickY && ballY <= brickY + brickHeight) {
                            bricks[i][j] = false;
                            ballDirectionY = -ballDirectionY;
                            score++;
                            if (isGameWon()) {
                                gameOver = true;
                                showGameOverDialog();
                            }
                        }
                    }
                }
            }

            // 공격 이동 및 충돌 감지
            for (int i = 0; i < numBricksX; i++) {
                if (attackActive[i]) {
                    attackY[i] += attackSpeed;
                    attackX[i] += attackDirectionX[i];
                    attackY[i] += attackDirectionY[i];

                    // 플레이어와의 충돌 감지
                    if (attackY[i] >= getHeight() - 20 && attackX[i] >= paddleX && attackX[i] <= paddleX + 100) {
                        playerHealth--;
                        if (playerHealth <= 0) {
                            gameOver = true;
                            showGameOverDialog();
                        } else {
                            attackActive[i] = false;
                        }
                    }

                    // 공격이 화면 밖으로 나갈 경우 비활성화
                    if (attackY[i] > getHeight()) {
                        attackActive[i] = false;
                    }
                }
            }

            // 공격 발사
            for (int i = 0; i < numBricksX; i++) {
                if (!attackActive[i] && Math.random() < 0.001) { // 랜덤한 확률로 공격 발사
                    attackActive[i] = true;
                    attackX[i] = i * (brickWidth + brickGap) + brickWidth / 2;
                    attackY[i] = 0;

                    // 플레이어를 향해 공격 방향 설정
                    int playerCenterX = paddleX + 50; // 플레이어의 중앙 X 좌표
                    int playerCenterY = getHeight() - 50; // 플레이어의 Y 좌표 (위치는 고정)

                    double angle = Math.atan2(playerCenterY - attackY[i], playerCenterX - attackX[i]);
                    attackDirectionX[i] = (int) (Math.cos(angle) * attackSpeed);
                    attackDirectionY[i] = (int) (Math.sin(angle) * attackSpeed);
                }
            }

            // 헬스팩 이동 및 충돌 감지
            for (int i = 0; i < numBricksX; i++) {
                if (healthPackActive[i]) {
                    healthPackY[i] += healthPackSpeed;
                    healthPackX[i] += healthPackDirectionX[i];
                    healthPackY[i] += healthPackDirectionY[i];

                    // 플레이어와의 충돌 감지
                    if (healthPackY[i] >= getHeight() - 20 && healthPackX[i] >= paddleX && healthPackX[i] <= paddleX + 100) {
                        if (playerHealth < 5) {
                            playerHealth++;
                            healthPackActive[i] = false;
                        }
                    }

                    // 헬스팩이 화면 밖으로 나갈 경우 비활성화
                    if (healthPackY[i] > getHeight()) {
                        healthPackActive[i] = false;
                    }
                }
            }

            // 헬스팩 발사
            for (int i = 0; i < numBricksX; i++) {
                if (!healthPackActive[i] && Math.random() < 0.0005) { // 랜덤한 확률로 공격 발사
                    healthPackActive[i] = true;
                    healthPackX[i] = i * (brickWidth + brickGap) + brickWidth / 2;
                    healthPackY[i] = 0;

                    // 플레이어를 향해 공격 방향 설정
                    int playerCenterX = paddleX + 50; // 플레이어의 중앙 X 좌표
                    int playerCenterY = getHeight() - 50; // 플레이어의 Y 좌표 (위치는 고정)

                    double angle = Math.atan2(playerCenterY - healthPackY[i], playerCenterX - healthPackX[i]);
                    healthPackDirectionX[i] = (int) (Math.cos(angle) * healthPackSpeed);
                    healthPackDirectionY[i] = (int) (Math.sin(angle) * healthPackSpeed);
                }
            }

            // 패들 이동
            paddleX += paddleDirectionX;
            if (paddleX <= 0) {
                paddleX = 0;
            }
            if (paddleX >= getWidth() - 100) {
                paddleX = getWidth() - 100;
            }

            repaint();
        }
    }

    private void showGameOverDialog() {
        String message = "게임 종료\n점수: " + score + "\n플레이어 체력: " + playerHealth + "\n다시 하시겠습니까?";
        int choice = JOptionPane.showConfirmDialog(this, message, "게임 종료", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            resetGame();
        } else {
            System.exit(0);
        }
    }

    private void resetGame() {
        // 게임 재설정
        score = 0;
        playerHealth = 5; // 플레이어 체력 초기화
        gameOver = false;

        // 공 및 벽돌 초기화
        ballX = 150;
        ballY = 300;
        ballDirectionX = 3;
        ballDirectionY = 3;

        for (int i = 0; i < numBricksX; i++) {
            for (int j = 0; j < numBricksY; j++) {
                bricks[i][j] = true;
            }
        }

        for (int i = 0; i < numBricksX; i++) {
            attackActive[i] = false;
            healthPackActive[i] = false;
        }

        repaint();
    }

    private boolean isGameWon() {
        // 모든 벽돌이 제거되면 게임 승리
        for (int i = 0; i < numBricksX; i++) {
            for (int j = 0; j < numBricksY; j++) {
                if (bricks[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 배경 그리기
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());

        // 벽돌 그리기
        for (int i = 0; i < numBricksX; i++) {
            for (int j = 0; j < numBricksY; j++) {
                if (bricks[i][j]) {
                    // 벽돌 색상 가져오기
                    Color brickColor = brickColors[i][j];
                    g.setColor(brickColor);

                    // 벽돌 그리기
                    int brickX = i * (brickWidth + brickGap);
                    int brickY = j * (brickHeight + brickGap);
                    g.fillRect(brickX, brickY, brickWidth, brickHeight);
                }
            }
        }

        // 패들 그리기
        g.setColor(Color.green);
        g.fillRect(paddleX, getHeight() - 20, 100, 10);

        // 공 그리기
        g.setColor(Color.red);
        g.fillOval(ballX, ballY, 20, 20);

        // 점수 그리기
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("점수: " + score, 10, 30);

        // 공격 그리기
        g.setColor(Color.red); // 빨간색으로 설정
        for (int i = 0; i < numBricksX; i++) {
            if (attackActive[i]) {
                g.fillRect(attackX[i], attackY[i], 10, 10); // 네모 모양 공격
            }
        }

        // 추가된 코드: 체력 팩 그리기
        g.setColor(Color.GREEN); // 빨간색으로 설정
        for (int i = 0; i < numBricksX; i++) {
            if (healthPackActive[i]) {
                g.fillRect(healthPackX[i], healthPackY[i], 10, 10); // 네모 모양 체력 팩
            }
        }


        // HP 바 그리기
        g.setColor(Color.white);
        g.fillRect(10, getHeight() - 40, 200, 20); // HP 바 배경
        g.setColor(Color.red);
        g.fillRect(10, getHeight() - 40, playerHealth * 40, 20); // HP 바

    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            paddleDirectionX = -5;
        }
        if (key == KeyEvent.VK_RIGHT) {
            paddleDirectionX = 5;
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            paddleDirectionX = 0;
        }
    }

    public void keyTyped(KeyEvent e) {}

}