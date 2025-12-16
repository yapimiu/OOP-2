package org.example;

import java.util.*;

public class CheckersGame {

    // Игровая доска 8x8 (каждая клетка хранит тип фигуры)
    private final Piece[][] board = new Piece[8][8];

    // Флаг очереди хода: true — ход белых, false — ход чёрных
    private boolean whiteTurn = true;

    // Текущая выбранная шашка (в selected храним координаты fromRow/fromCol)
    private Move selected = null;

    // Текущий набор возможных ходов (либо для выбранной шашки, либо общий — см. updatePossibleMoves)
    private final Set<Move> possibleMoves = new HashSet<>();

    // Идёт ли серия взятий (когда после взятия нужно продолжать бить той же фигурой)
    private boolean inCaptureSequence = false;

    // Конструктор: старт новой игры
    public CheckersGame() {
        resetGame();
    }

    // Сброс состояния игры и обновление всех ходов
    public void resetGame() {
        whiteTurn = true;                 // Начинают белые
        selected = null;                  // Снимаем выделение
        inCaptureSequence = false;        // Серии взятий нет
        initBoard();                      // Расставляем фигуры
        updatePossibleMoves();            // Пересчитываем доступные ходы
    }

    // Инициализация стандартной начальной позиции
    private void initBoard() {
        // Заполняем всё пустыми клетками
        for (int r = 0; r < 8; r++) Arrays.fill(board[r], Piece.EMPTY);

        // Расставляем чёрные шашки (верхние 3 ряда по тёмным клеткам)
        for (int r = 0; r < 3; r++)
            for (int c = (r + 1) % 2; c < 8; c += 2)
                board[r][c] = Piece.BLACK_MAN;

        // Расставляем белые шашки (нижние 3 ряда по тёмным клеткам)
        for (int r = 5; r < 8; r++)
            for (int c = (r + 1) % 2; c < 8; c += 2)
                board[r][c] = Piece.WHITE_MAN;
    }

    // Получить фигуру на клетке
    public Piece get(int r, int c) { return board[r][c]; }

    // Чей ход сейчас
    public boolean isWhiteTurn() { return whiteTurn; }

    // Какая шашка выбрана (координаты лежат в fromRow/fromCol)
    public Move getSelected() { return selected; }

    // Текущий набор ходов (часто — для подсветки в UI)
    public Set<Move> getPossibleMoves() { return possibleMoves; }

    // Текст для интерфейса
    public String getTurnText() { return whiteTurn ? "Ход белых" : "Ход чёрных"; }

    // Обработка клика по клетке (выбор шашки или попытка хода)
    public void click(int row, int col) {
        if (selected == null) {
            // Если ничего не выбрано — пробуем выбрать шашку
            selectPiece(row, col);
        } else {
            // Если шашка выбрана — пробуем сходить, иначе перевыбираем
            if (!tryMove(row, col)) {
                selectPiece(row, col);
            }
        }
    }

    // Выбор шашки и расчёт её ходов с учётом правила обязательного взятия
    private void selectPiece(int row, int col) {
        // В серии взятий нельзя переключаться на другую шашку
        if (inCaptureSequence && (selected == null || row != selected.fromRow || col != selected.fromCol)) return;

        Piece p = board[row][col];

        // Нельзя выбрать пустую клетку или шашку противника
        if (p == Piece.EMPTY || p.isWhite() != whiteTurn) {
            selected = null;
            possibleMoves.clear();
            return;
        }

        // Проверяем, обязателен ли бой у игрока в целом
        boolean mustCapture = playerMustCapture();

        // Считаем взятия именно для этой шашки
        List<Move> captures = p.isKing() ? findKingCaptures(row, col) : findManCaptures(row, col);

        possibleMoves.clear();

        if (mustCapture) {
            // Если обязателен бой — разрешаем выбрать только шашку, которая может бить
            if (!captures.isEmpty()) {
                possibleMoves.addAll(captures);
                selected = new Move(row, col, -1, -1); // toRow/toCol тут не важны — это "маркер выбора"
            } else {
                selected = null;
            }
        } else {
            // Если бой не обязателен — добавляем и взятия, и тихие ходы
            possibleMoves.addAll(captures);
            possibleMoves.addAll(p.isKing() ? findKingQuietMoves(row, col) : findManQuietMoves(row, col));
            if (!possibleMoves.isEmpty()) selected = new Move(row, col, -1, -1);
        }
    }

    // Попытка выполнить ход выбранной шашкой в (toRow, toCol)
    private boolean tryMove(int toRow, int toCol) {
        if (selected == null) return false;

        // Находим среди possibleMoves ход, соответствующий выбранной шашке и целевой клетке
        Move chosenMove = null;
        for (Move move : possibleMoves) {
            if (move.fromRow == selected.fromRow && move.fromCol == selected.fromCol &&
                    move.toRow == toRow && move.toCol == toCol) {
                chosenMove = move;
                break;
            }
        }
        if (chosenMove == null) return false;

        // Запоминаем, был ли это бой (чтобы решить — продолжать ли серию)
        boolean wasCapture = chosenMove.isCapture;

        // Выполняем перемещение и удаление побитых
        executeMove(chosenMove);

        if (wasCapture) {
            // После взятия проверяем: может ли эта же шашка бить дальше (серия взятий)
            Piece movedPiece = board[toRow][toCol];
            List<Move> furtherCaptures = movedPiece.isKing()
                    ? findKingCaptures(toRow, toCol)
                    : findManCaptures(toRow, toCol);

            // Если можно бить дальше — не меняем ход, принудительно оставляем выделение
            if (!furtherCaptures.isEmpty()) {
                selected = new Move(toRow, toCol, -1, -1);
                possibleMoves.clear();
                possibleMoves.addAll(furtherCaptures);
                inCaptureSequence = true;
                return true;
            }
        }

        // Если серия взятий закончилась (или был тихий ход) — передаём ход другому игроку
        whiteTurn = !whiteTurn;
        inCaptureSequence = false;
        selected = null;

        // Пересчитываем все доступные ходы для нового игрока
        updatePossibleMoves();
        return true;
    }

    // Непосредственное выполнение хода: перенос фигуры, удаление побитых, превращение в дамку
    private void executeMove(Move m) {
        Piece p = board[m.fromRow][m.fromCol];

        // Освобождаем стартовую клетку
        board[m.fromRow][m.fromCol] = Piece.EMPTY;

        // Перемещаем фигуру на целевую клетку
        board[m.toRow][m.toCol] = p;

        if (m.isCapture) {
            // Удаляем все клетки по диагонали между from и to (для дамки — может быть больше 1 клетки)
            int dr = Integer.signum(m.toRow - m.fromRow);
            int dc = Integer.signum(m.toCol - m.fromCol);
            int steps = Math.max(Math.abs(m.toRow - m.fromRow), Math.abs(m.toCol - m.fromCol));
            for (int i = 1; i < steps; i++) {
                board[m.fromRow + dr * i][m.fromCol + dc * i] = Piece.EMPTY;
            }
        }

        // Превращение в дамку при достижении последней линии
        if ((p == Piece.WHITE_MAN && m.toRow == 0) || (p == Piece.BLACK_MAN && m.toRow == 7)) {
            board[m.toRow][m.toCol] = (p == Piece.WHITE_MAN) ? Piece.WHITE_KING : Piece.BLACK_KING;
        }
    }

    // Проверка правила обязательного взятия: есть ли у текущего игрока хотя бы одно взятие
    private boolean playerMustCapture() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    // Если хотя бы одна фигура может бить — бой обязателен
                    if (!(p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c)).isEmpty()) return true;
                }
            }
        }
        return false;
    }

    // --- Поиск ходов для обычной шашки (тихие ходы) ---
    private List<Move> findManQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();

        // Белые ходят "вверх" (к 0-й строке), чёрные — "вниз" (к 7-й строке)
        int dir = (board[r][c] == Piece.WHITE_MAN) ? -1 : 1;

        // У обычной шашки 2 направления по диагонали
        for (int dc = -1; dc <= 1; dc += 2) {
            if (isValid(r + dir, c + dc) && board[r + dir][c + dc] == Piece.EMPTY)
                moves.add(new Move(r, c, r + dir, c + dc, false));
        }
        return moves;
    }

    // --- Поиск взятий для обычной шашки ---
    private List<Move> findManCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        Piece current = board[r][c];

        // Взятие возможно во всех 4 диагональных направлениях (в данной реализации)
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                // Для взятия нужно перескочить через соседнюю клетку
                if (isValid(r + dr * 2, c + dc * 2)) {
                    Piece mid = board[r + dr][c + dc];
                    // В середине должен стоять противник, а конечная клетка — быть пустой
                    if (mid != Piece.EMPTY && mid.isWhite() != current.isWhite() &&
                            board[r + dr * 2][c + dc * 2] == Piece.EMPTY) {
                        moves.add(new Move(r, c, r + dr * 2, c + dc * 2, true));
                    }
                }
            }
        }
        return moves;
    }

    // --- Тихие ходы дамки: скольжение по диагонали на любую дистанцию ---
    private List<Move> findKingQuietMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();

        // Дамка ходит по 4 диагоналям
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                // Дамка может идти на 1..7 клеток, пока не упрётся
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist, nc = c + dc * dist;
                    // За границы нельзя, на занятую клетку нельзя
                    if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                    moves.add(new Move(r, c, nr, nc, false));
                }
            }
        }
        return moves;
    }

    // --- Взятия дамки: находим первую вражескую фигуру по диагонали и любые посадочные клетки за ней ---
    private List<Move> findKingCaptures(int r, int c) {
        List<Move> moves = new ArrayList<>();
        Piece current = board[r][c];

        // Для каждого диагонального направления ищем первую вражескую шашку
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int eR = -1, eC = -1;

                // Шаг 1: найти первую непустую клетку на диагонали
                for (int dist = 1; dist < 8; dist++) {
                    int nr = r + dr * dist, nc = c + dc * dist;
                    if (!isValid(nr, nc)) break;

                    Piece t = board[nr][nc];
                    if (t == Piece.EMPTY) continue;

                    // Своя фигура блокирует диагональ
                    if (t.isWhite() == current.isWhite()) break;

                    // Нашли вражескую фигуру — потенциальное взятие
                    eR = nr; eC = nc;
                    break;
                }

                // Шаг 2: после найденного врага добавляем все пустые клетки дальше как возможные landing
                if (eR != -1) {
                    for (int dist = 1; dist < 8; dist++) {
                        int nr = eR + dr * dist, nc = eC + dc * dist;
                        if (!isValid(nr, nc) || board[nr][nc] != Piece.EMPTY) break;
                        moves.add(new Move(r, c, nr, nc, true));
                    }
                }
            }
        }
        return moves;
    }

    // Проверка координат на попадание в диапазон 0..7
    private boolean isValid(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    // Пересчёт всех доступных ходов для текущего игрока (в целом по доске)
    private void updatePossibleMoves() {
        possibleMoves.clear();    // Сбрасываем предыдущие ходы
        selected = null;          // Сбрасываем выделение
        inCaptureSequence = false;// Сбрасываем серию взятий (новый расчёт)

        boolean mustCapture = playerMustCapture();

        // Собираем ходы всех фигур текущего игрока
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != Piece.EMPTY && p.isWhite() == whiteTurn) {
                    List<Move> cMoves = p.isKing() ? findKingCaptures(r, c) : findManCaptures(r, c);
                    List<Move> qMoves = p.isKing() ? findKingQuietMoves(r, c) : findManQuietMoves(r, c);

                    // Если бой обязателен — добавляем только взятия
                    if (mustCapture) possibleMoves.addAll(cMoves);
                    else {
                        // Иначе — и взятия, и тихие ходы
                        possibleMoves.addAll(cMoves);
                        possibleMoves.addAll(qMoves);
                    }
                }
            }
        }
    }

    // --- ИИ: делает простой случайный ход из possibleMoves ---
    public void makeAIMove() {
        if (possibleMoves.isEmpty()) return;

        // Разделяем ходы на взятия и тихие, чтобы предпочесть взятие
        List<Move> captures = new ArrayList<>();
        List<Move> quiets = new ArrayList<>();
        for (Move m : possibleMoves) {
            if (m.isCapture) captures.add(m); else quiets.add(m);
        }

        // Выбираем случайный ход (приоритет — взятия)
        Move chosen = !captures.isEmpty()
                ? captures.get(new Random().nextInt(captures.size()))
                : quiets.get(new Random().nextInt(quiets.size()));

        // "Выделяем" шашку ИИ и выполняем ход через общую логику tryMove (с сериями взятий)
        selected = new Move(chosen.fromRow, chosen.fromCol, -1, -1);
        tryMove(chosen.toRow, chosen.toCol);
    }

    // Возвращает имя победителя или null, если игра продолжается
    public String getWinner() {
        boolean hasWhite = false, hasBlack = false;

        // Проверяем, остались ли на доске шашки обеих сторон
        for (Piece[] row : board) {
            for (Piece p : row) {
                if (p.isWhite()) hasWhite = true;
                if (p.isBlack()) hasBlack = true;
            }
        }

        // Если белых не осталось — победили чёрные
        if (!hasWhite) return "Чёрные";

        // Если чёрных не осталось — победили белые
        if (!hasBlack) return "Белые";

        // Если у текущего игрока нет ходов — он проиграл (победа другого)
        if (possibleMoves.isEmpty()) {
            return whiteTurn ? "Чёрные" : "Белые";
        }

        // Игра продолжается
        return null;
    }
}
