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
    private String contentStr;
    // The symbol which is used to connect child nodes, default '\0'
    private char calculateSymbol = '\0';
    // Child node array
    private GrammarNode[] childNodes = new GrammarNode[0];

    public GrammarNode(String content) throws NormalFormException {
        if (content != null && !content.isEmpty()) {
            buildNode(content);
        } else {
            throw new NormalFormException("Content is empty!");
        }
    }

    private static String deleteDuplicatedOutsideParenthesis(String content) {
        StringBuilder builder = new StringBuilder(content);
        return deleteDuplicatedOutsideParenthesis(builder);
    }

    private static String deleteDuplicatedOutsideParenthesis(StringBuilder builder) {
        int parenthesisCounter = 0;
        int charLength = builder.length();
        while (builder.charAt(0) == Symbol.PARENTHESIS_LEFT && builder.charAt(charLength - 1) == Symbol.PARENTHESIS_RIGHT) {
            boolean hasDuplicatedParenthesis = true;
            for (int i = 1; i < charLength - 1; i++) {
                if (builder.charAt(i) == Symbol.PARENTHESIS_LEFT) {
                    parenthesisCounter++;
                } else if (builder.charAt(i) == Symbol.PARENTHESIS_RIGHT) {
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
                builder.deleteCharAt(charLength - 1);
                builder.deleteCharAt(0);

                parenthesisCounter = 0;
                charLength = builder.length();
            } else {
                break;
            }
        }
        return builder.toString();
    }

    private static String grammarContentCleanAndCheck(String content) throws NormalFormException {
        StringBuilder builder = new StringBuilder(content.length());
        char[] charArr = content.toCharArray();
        char lastWord = ' ';
        int parenthesisCounter = 0;
        for (char c : charArr) {
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
                    builder.append(Symbol.AND);
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
                        builder.deleteCharAt(builder.length() - 1);
                        if (builder.length() < 1) {
                            lastWord = ' ';
                        } else {
                            lastWord = builder.charAt(builder.length() - 1);
                        }
                        continue;
                    }
                }
                // Append Words
                builder.append(c);
                lastWord = c;
            }
        }

        // Symbol PARENTHESIS_LEFT use error
        if (parenthesisCounter > 0) {
            throw new NormalFormException("Symbol " + Symbol.PARENTHESIS_LEFT + " use incorrectly");
        }

        int charLength = builder.length();

        for (int i = 0; i < charLength; i++) {
            if (builder.charAt(i) == Symbol.SELF_LOOP) { // Symbol SELF_LOOP use check
                if (i == 0
                        || builder.charAt(i - 1) == Symbol.AND
                        || builder.charAt(i - 1) == Symbol.OR
                        || builder.charAt(i - 1) == Symbol.SELF_LOOP
                        || builder.charAt(i - 1) == Symbol.PARENTHESIS_LEFT) {
                    throw new NormalFormException("Symbol " + Symbol.SELF_LOOP + " use incorrectly");
                }
            } else if (builder.charAt(i) == Symbol.OR || builder.charAt(i) == Symbol.AND) { // Symbol OR and AND use check
                if (i == 0 || i == charLength - 1
                        || builder.charAt(i - 1) == Symbol.AND
                        || builder.charAt(i - 1) == Symbol.OR
                        || builder.charAt(i - 1) == Symbol.PARENTHESIS_LEFT
                        || builder.charAt(i + 1) == Symbol.AND
                        || builder.charAt(i + 1) == Symbol.OR
                        || builder.charAt(i + 1) == Symbol.SELF_LOOP
                        || builder.charAt(i + 1) == Symbol.PARENTHESIS_RIGHT) {
                    throw new NormalFormException("Symbol " + builder.charAt(i) + " use incorrectly");
                }
            }
        }

        return deleteDuplicatedOutsideParenthesis(builder);
    }

    private void divideChildNodes(String content) throws NormalFormException {
        boolean hasAnd = false;
        boolean hasOr = false;
        int andCounter = 0;
        int orCounter = 0;
        char[] charArr = content.toCharArray();

        // Delete inside parenthesises and get calculate symbol
        int parenthesisCounter = 0;
        boolean foundLeftParenthesis = false;
        for (char c : charArr) {
            if (c == Symbol.PARENTHESIS_LEFT) {
                if (parenthesisCounter == 0) {
                    foundLeftParenthesis = true;
                }
                parenthesisCounter++;
            } else if (c == Symbol.PARENTHESIS_RIGHT) {
                parenthesisCounter--;
                if (parenthesisCounter == 0) {
                    foundLeftParenthesis = false;
                }
            } else if (!foundLeftParenthesis) {
                if (c == Symbol.AND) {
                    andCounter++;
                    hasAnd = true;
                } else if (c == Symbol.OR) {
                    orCounter++;
                    hasOr = true;
                }
            }
        }

        if (hasOr) {
            this.calculateSymbol = Symbol.OR;
        } else if (hasAnd) {
            this.calculateSymbol = Symbol.AND;
        }

        if (this.calculateSymbol != '\0') {
            int startCut = 0;
            int childNodeCounter = 0;
            this.childNodes = new GrammarNode[this.calculateSymbol == Symbol.OR ? orCounter + 1 : andCounter + 1];
            foundLeftParenthesis = false;
            parenthesisCounter = 0;
            for (int i = 0; i < charArr.length; i++) {
                if (charArr[i] == Symbol.PARENTHESIS_LEFT) {
                    if (parenthesisCounter == 0) {
                        foundLeftParenthesis = true;
                    }
                    parenthesisCounter++;
                } else if (charArr[i] == Symbol.PARENTHESIS_RIGHT) {
                    parenthesisCounter--;
                    if (parenthesisCounter == 0) {
                        foundLeftParenthesis = false;
                    }
                }
                if (!foundLeftParenthesis) {
                    // Divide Nodes
                    if (charArr[i] == this.calculateSymbol || i == charArr.length - 1) {
                        String childNodeContent = content.substring(startCut, i == charArr.length - 1 ? i + 1 : i);
                        startCut = i + 1;
                        this.childNodes[childNodeCounter] = new GrammarNode(childNodeContent);
                        childNodeCounter++;
                    }
                }
            }
        } else {
            // Check Leaf Node
            boolean hasParenthesis = false;
            for (char c : charArr) {
                if (c == Symbol.PARENTHESIS_RIGHT || c == Symbol.PARENTHESIS_LEFT) {
                    hasParenthesis = true;
                    break;
                }
            }
            if (hasParenthesis) {
                this.childNodes = new GrammarNode[]{
                        new GrammarNode(content)
                };
            } else {
                this.isLeafNode = true;
            }
        }
    }

    private void checkAndCleanParenthesisLoopNode() {
        char[] charArr = this.contentStr.toCharArray();
        boolean mayLoopNode = charArr[charArr.length - 1] == Symbol.SELF_LOOP;
        if (mayLoopNode) {
            if (charArr.length > 3 && charArr[0] == Symbol.PARENTHESIS_LEFT && charArr[charArr.length - 2] == Symbol.PARENTHESIS_RIGHT) {
                int parenthesisCounter = 0;
                boolean parenthesisError = false;
                for (int i = 1; i < charArr.length - 2; i++) {
                    if (charArr[i] == Symbol.PARENTHESIS_LEFT) {
                        parenthesisCounter++;
                    } else if (charArr[i] == Symbol.PARENTHESIS_RIGHT) {
                        parenthesisCounter--;

                        if (parenthesisCounter < 0) {
                            parenthesisError = true;
                            break;
                        }
                    }
                }

                if (!parenthesisError && parenthesisCounter == 0) {
                    this.isLoopNode = true;
                    this.contentStr = this.contentStr.substring(1, this.contentStr.length() - 2);
                    this.contentStr = deleteDuplicatedOutsideParenthesis(this.contentStr);
                }
            }
        }
    }

    private void checkAndCleanLoopNode() {
        char[] charArr = this.contentStr.toCharArray();
        boolean mayLoopNode = charArr[charArr.length - 1] == Symbol.SELF_LOOP;
        if (mayLoopNode && isLeafNode) {
            this.isLoopNode = true;
            this.contentStr = this.contentStr.substring(0, this.contentStr.length() - 1);
            this.contentStr = deleteDuplicatedOutsideParenthesis(this.contentStr);
        }
    }

    private void buildNode(String content) throws NormalFormException {
        this.contentStr = grammarContentCleanAndCheck(content);
        checkAndCleanParenthesisLoopNode();
        divideChildNodes(this.contentStr);
        checkAndCleanLoopNode();
    }

    public boolean isLeafNode() {
        return isLeafNode;
    }

    public boolean isLoopNode() {
        return isLoopNode;
    }

    public String getContentStr() {
        return this.contentStr;
    }

    public char getCalculateSymbol() {
        return this.calculateSymbol;
    }

    public GrammarNode[] getChildNodes() {
        return this.childNodes;
    }

    public int getChildCount() {
        return this.childNodes.length;
    }

    @Override
    public String toString() {
        return "GrammarNode{" +
                "isLeafNode=" + isLeafNode +
                ", isLoopNode=" + isLoopNode +
                ", contentStr=\"" + contentStr + "\"" +
                ", calculateSymbol=" + (calculateSymbol == '\0' ? null : "\"" + calculateSymbol + "\"") +
                ", childNodes=" + Arrays.toString(childNodes) +
                '}';
    }
}
