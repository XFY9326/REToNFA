import java.util.Arrays;

/**
 * Regular Expression Grammar Node
 * Use Tree Structure
 *
 * @author xfy9326
 */
public class GrammarNode {
    // Leaf node (No child nodes)
    private boolean isLeafNode = false;
    // If there are symbol SELF_LOOP behind this node
    private boolean isLoopNode = false;
    // Original content (if it's leaf node, it will be leaf content)
    private char[] contentForm;
    // The symbol which is used to connect child nodes, default '\0'
    private char calculateSymbol = '\0';
    // Is root node
    private boolean isRootNode;
    // Child node array
    private GrammarNode[] childNodes = new GrammarNode[0];

    public GrammarNode(String content, boolean isRootNode) throws NormalFormException {
        this(content.toCharArray(), isRootNode);
    }

    private GrammarNode(char[] content) throws NormalFormException {
        this(content, false);
    }

    public GrammarNode(char[] content, boolean isRootNode) throws NormalFormException {
        if (content != null && content.length > 0) {
            this.isRootNode = isRootNode;
            buildNode(content);
        } else {
            throw new NormalFormException("Content is empty!");
        }
    }

    private static char[] grammarContentCleanAndCheck(char[] charArr) throws NormalFormException {
        char[] builder = new char[charArr.length * 2];
        int builderSize = 0;

        char lastWord = ' ';
        int parenthesisCounter = 0;
        for (int i = 0; i < charArr.length; i++) {
            char c = charArr[i];

            // Delete useless char
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                // Add AND Symbol
                if (lastWord != ' '
                        && lastWord != Symbol.AND
                        && lastWord != Symbol.OR
                        && lastWord != Symbol.PARENTHESIS_LEFT
                        && c != Symbol.AND
                        && c != Symbol.OR
                        && c != Symbol.SELF_LOOP
                        && c != Symbol.PARENTHESIS_RIGHT) {
                    builder[builderSize++] = Symbol.AND;
                }

                if (c == Symbol.PARENTHESIS_LEFT) { // Symbol PARENTHESIS_LEFT use check
                    parenthesisCounter++;
                } else if (c == Symbol.PARENTHESIS_RIGHT) { // Symbol PARENTHESIS_RIGHT use check
                    parenthesisCounter--;

                    // Symbol PARENTHESIS_RIGHT use error
                    if (parenthesisCounter < 0) {
                        throw new NormalFormException("Symbol " + Symbol.PARENTHESIS_RIGHT + " use incorrectly");
                    }

                    if (lastWord == Symbol.PARENTHESIS_LEFT) { // Delete () use
                        builderSize--;
                        if (builderSize == 0) {
                            lastWord = ' ';
                        } else {
                            lastWord = builder[builderSize - 1];
                        }
                        continue;
                    }
                }

                if (c == Symbol.SELF_LOOP) { // Symbol SELF_LOOP use check
                    if (builderSize == 0
                            || lastWord == Symbol.AND
                            || lastWord == Symbol.OR
                            || lastWord == Symbol.SELF_LOOP
                            || lastWord == Symbol.PARENTHESIS_LEFT) {
                        throw new NormalFormException("Symbol " + Symbol.SELF_LOOP + " use incorrectly");
                    }
                } else if (c == Symbol.OR || c == Symbol.AND) { // Symbol OR and AND use check
                    if (builderSize == 0 || i == charArr.length - 1
                            || lastWord == Symbol.AND
                            || lastWord == Symbol.OR
                            || lastWord == Symbol.PARENTHESIS_LEFT
                            || charArr[i + 1] == Symbol.AND
                            || charArr[i + 1] == Symbol.OR
                            || charArr[i + 1] == Symbol.SELF_LOOP
                            || charArr[i + 1] == Symbol.PARENTHESIS_RIGHT) {
                        throw new NormalFormException("Symbol " + c + " use incorrectly");
                    }
                }
                // Append Words
                builder[builderSize++] = c;
                lastWord = c;
            }
        }

        // Symbol PARENTHESIS_LEFT use error
        if (parenthesisCounter > 0) {
            throw new NormalFormException("Symbol " + Symbol.PARENTHESIS_LEFT + " use incorrectly");
        }

        return deleteDuplicatedOutsideParenthesis(builder, 0, builderSize);
    }

    /**
     * ((A)) -> A
     *
     * @param charArr content
     * @param start   startPosition
     * @param arrSize contentSize
     * @return content
     */
    private static char[] deleteDuplicatedOutsideParenthesis(char[] charArr, int start, int arrSize) {
        int parenthesisCounter = 0;
        while (charArr[start] == Symbol.PARENTHESIS_LEFT && charArr[arrSize - 1] == Symbol.PARENTHESIS_RIGHT) {
            boolean hasDuplicatedParenthesis = true;
            for (int i = 1; i < arrSize - 1; i++) {
                if (charArr[i] == Symbol.PARENTHESIS_LEFT) {
                    parenthesisCounter++;
                } else if (charArr[i] == Symbol.PARENTHESIS_RIGHT) {
                    parenthesisCounter--;
                    if (parenthesisCounter < 0) {
                        hasDuplicatedParenthesis = false;
                        break;
                    }
                }
            }
            if (parenthesisCounter > 0) {
                hasDuplicatedParenthesis = false;
            }
            if (hasDuplicatedParenthesis) {
                arrSize--;
                start++;

                parenthesisCounter = 0;
            } else {
                break;
            }
        }
        return Arrays.copyOfRange(charArr, start, arrSize);
    }

    private void divideChildNodes(char[] charArr) throws NormalFormException {
        boolean hasAnd = false;
        boolean hasOr = false;
        int[] cutPosition = new int[charArr.length / 2 + 1];
        int andCounter = 0;
        int orCounter = 0;

        // Delete inside parenthesises and get calculate symbol
        int parenthesisCounter = 0;
        boolean hasParenthesis = false;
        boolean foundLeftParenthesis = false;
        for (int i = 0; i < charArr.length; i++) {
            char c = charArr[i];
            if (c == Symbol.PARENTHESIS_LEFT) {
                if (parenthesisCounter == 0) {
                    foundLeftParenthesis = true;
                }
                parenthesisCounter++;
                hasParenthesis = true;
            } else if (c == Symbol.PARENTHESIS_RIGHT) {
                parenthesisCounter--;
                if (parenthesisCounter == 0) {
                    foundLeftParenthesis = false;
                }
            } else if (!foundLeftParenthesis) {
                if (!hasOr && c == Symbol.AND) {
                    cutPosition[andCounter++] = i;
                    hasAnd = true;
                } else if (c == Symbol.OR) {
                    cutPosition[orCounter++] = i;
                    hasOr = true;
                }
            }
        }

        int cutSize;
        if (hasOr) {
            cutSize = orCounter;
            this.calculateSymbol = Symbol.OR;
        } else if (hasAnd) {
            cutSize = andCounter;
            this.calculateSymbol = Symbol.AND;
        } else {
            // Check Leaf Node
            if (hasParenthesis) {
                this.childNodes = new GrammarNode[]{
                        new GrammarNode(charArr)
                };
            } else {
                this.isLeafNode = true;
            }
            return;
        }

        int startCut = 0;
        this.childNodes = new GrammarNode[cutSize + 1];
        for (int i = 0; i <= cutSize; i++) {
            // Divide Nodes
            char[] childNodeContent;
            if (i == cutSize) {
                childNodeContent = Arrays.copyOfRange(charArr, startCut, charArr.length);
            } else {
                childNodeContent = Arrays.copyOfRange(charArr, startCut, cutPosition[i]);
                startCut = cutPosition[i] + 1;
            }
            this.childNodes[i] = new GrammarNode(childNodeContent);
        }
    }

    /**
     * Check (A)*
     */
    private void checkAndCleanParenthesisLoopNode() {
        if (contentForm[contentForm.length - 1] == Symbol.SELF_LOOP) {
            if (contentForm.length > 3 && contentForm[0] == Symbol.PARENTHESIS_LEFT && contentForm[contentForm.length - 2] == Symbol.PARENTHESIS_RIGHT) {
                int parenthesisCounter = 0;
                boolean parenthesisError = false;
                for (int i = 1; i < contentForm.length - 2; i++) {
                    if (contentForm[i] == Symbol.PARENTHESIS_LEFT) {
                        parenthesisCounter++;
                    } else if (contentForm[i] == Symbol.PARENTHESIS_RIGHT) {
                        parenthesisCounter--;

                        if (parenthesisCounter < 0) {
                            parenthesisError = true;
                            break;
                        }
                    }
                }

                if (!parenthesisError && parenthesisCounter == 0) {
                    this.isLoopNode = true;
                    this.contentForm = deleteDuplicatedOutsideParenthesis(this.contentForm, 1, this.contentForm.length - 2);
                }
            }
        }
    }

    /**
     * Check A*
     */
    private void checkAndCleanLoopNode() {
        if (contentForm[contentForm.length - 1] == Symbol.SELF_LOOP && isLeafNode) {
            this.isLoopNode = true;
            this.contentForm = deleteDuplicatedOutsideParenthesis(this.contentForm, 0, this.contentForm.length - 1);
        }
    }

    private void buildNode(char[] charArr) throws NormalFormException {
        if (this.isRootNode) {
            this.contentForm = grammarContentCleanAndCheck(charArr);
        } else {
            this.contentForm = deleteDuplicatedOutsideParenthesis(charArr, 0, charArr.length);
        }
        checkAndCleanParenthesisLoopNode();
        divideChildNodes(this.contentForm);
        checkAndCleanLoopNode();
    }

    public boolean isLeafNode() {
        return isLeafNode;
    }

    public boolean isLoopNode() {
        return isLoopNode;
    }

    public String getContentForm() {
        return new String(this.contentForm);
    }

    public char getCalculateSymbol() {
        return this.calculateSymbol;
    }

    public GrammarNode[] getChildNodes() {
        return this.childNodes;
    }

    @Override
    public String toString() {
        return "GrammarNode{" +
                "isRootNode=" + isRootNode +
                "isLeafNode=" + isLeafNode +
                ", isLoopNode=" + isLoopNode +
                ", contentForm=\"" + getContentForm() + "\"" +
                ", calculateSymbol=" + (calculateSymbol == '\0' ? null : "\"" + calculateSymbol + "\"") +
                ", childNodes=" + Arrays.toString(childNodes) +
                '}';
    }
}
