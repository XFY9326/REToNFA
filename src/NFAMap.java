import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author xfy9326
 */
public class NFAMap {
    private static final String DEFINE_JSON_STR = "const NFAMapJSON=";
    private static final String OUTPUT_PATH = "";
    private static final String OUTPUT_JS_NAME = "NFA.js";
    private static final String OUTPUT_JS_FOLDER = "NFAMap" + File.separator + "js";
    private static final String[] RESOURCES_NFA_MAP = {"NFAMap/index.html", "NFAMap/js/d3.v4.min.js", "NFAMap/js/dagre-d3.min.js"};
    private Node[] nodes;
    private String startNode;
    private String endNode;
    private String[] normalNodeList;
    private String originalRegularExpression;

    private NFAMap() {
    }

    public Node[] getNodes() {
        return nodes;
    }

    public String getStartNode() {
        return startNode;
    }

    public String getEndNode() {
        return endNode;
    }

    private String getJSON() {
        JSONObject object = new JSONObject();
        object.put("originalRegularExpression", originalRegularExpression);
        object.put("startNode", startNode);
        object.put("endNode", endNode);
        JSONArray nodeArray = new JSONArray();
        Collections.addAll(nodeArray, normalNodeList);
        object.put("normalNodeList", nodeArray);
        JSONArray edgeArray = new JSONArray();
        for (Node edge : nodes) {
            JSONObject nodeObject = new JSONObject();
            nodeObject.put("fromNode", edge.from);
            nodeObject.put("toNode", edge.to);
            nodeObject.put("symbol", edge.symbol);
            edgeArray.add(nodeObject);
        }
        object.put("edgeList", edgeArray);
        return object.toJSONString();
    }

    public void outputAsHtml() {
        try {
            for (String resourceName : RESOURCES_NFA_MAP) {
                File outputFile = new File(OUTPUT_PATH + resourceName);
                if (!outputFile.getParentFile().exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    outputFile.getParentFile().mkdirs();
                }
                if (outputFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    outputFile.delete();
                }
                try (InputStream inputStream = getClass().getResourceAsStream(resourceName)) {
                    Files.copy(inputStream, outputFile.toPath());
                }
            }

            try (FileOutputStream outputStream = new FileOutputStream(OUTPUT_PATH + OUTPUT_JS_FOLDER + File.separator + OUTPUT_JS_NAME)) {
                String output = DEFINE_JSON_STR + getJSON();
                outputStream.write(output.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "NFAMap{" +
                "nodes=" + Arrays.toString(nodes) +
                ", startNode=\"" + startNode + "\"" +
                ", endNode=\"" + endNode + "\"" +
                ", normalNodeList=" + Arrays.toString(normalNodeList) +
                '}';
    }

    public String[] getNormalNodeList() {
        return normalNodeList;
    }

    public static class Builder {
        private static final String EPSILON = "ε";
        private final ArrayList<Node> nodeArrayList = new ArrayList<>();
        private GrammarNode node;
        private int statusCounter = 0;

        public Builder(GrammarNode node) throws NormalFormException {
            if (node != null) {
                this.node = node;
            } else {
                throw new NormalFormException("This node can't be built!");
            }
        }

        private int getNewStatusNum() {
            return this.statusCounter++;
        }

        private void addNode(int from, String symbol, int to) {
            this.nodeArrayList.add(new Node(String.valueOf(from), symbol, String.valueOf(to)));
        }

        /**
         * Get NFA Node List
         *
         * @param currentNode Grammar Node
         * @param start       Start status
         * @param end         End status
         */
        private void recursionGrammarNodes(GrammarNode currentNode, int start, int end) {
            // Loop
            if (currentNode.isLoopNode()) {
                int mid = getNewStatusNum();
                addNode(start, EPSILON, mid);
                addNode(mid, EPSILON, end);

                // Loop and Leaf
                if (currentNode.isLeafNode()) {
                    addNode(mid, currentNode.getContentForm(), mid);
                } else {
                    // Loop and not Leaf
                    if (currentNode.getCalculateSymbol() == Symbol.AND) {
                        GrammarNode[] nodes = currentNode.getChildNodes();
                        int childStart = mid;
                        int childEnd;
                        for (int i = 0; i < nodes.length; i++) {
                            if (i == nodes.length - 1) {
                                childEnd = mid;
                            } else {
                                childEnd = getNewStatusNum();
                            }
                            recursionGrammarNodes(nodes[i], childStart, childEnd);
                            childStart = childEnd;
                        }
                    } else if (currentNode.getCalculateSymbol() == Symbol.OR) {
                        for (GrammarNode childNode : currentNode.getChildNodes()) {
                            recursionGrammarNodes(childNode, mid, mid);
                        }
                    } else {
                        GrammarNode childNode = currentNode.getChildNodes()[0];
                        recursionGrammarNodes(childNode, mid, mid);
                    }
                }
            } else {
                // Not Loop and Leaf
                if (currentNode.isLeafNode()) {
                    addNode(start, currentNode.getContentForm(), end);
                } else {
                    // Not Loop and not Leaf
                    if (currentNode.getCalculateSymbol() == Symbol.AND) {
                        GrammarNode[] nodes = currentNode.getChildNodes();
                        int childStart = start;
                        int childEnd;
                        for (int i = 0; i < nodes.length; i++) {
                            if (i == nodes.length - 1) {
                                childEnd = end;
                            } else {
                                childEnd = getNewStatusNum();
                            }
                            recursionGrammarNodes(nodes[i], childStart, childEnd);
                            childStart = childEnd;
                        }
                    } else if (currentNode.getCalculateSymbol() == Symbol.OR) {
                        for (GrammarNode childNode : currentNode.getChildNodes()) {
                            recursionGrammarNodes(childNode, start, end);
                        }
                    } else {
                        GrammarNode childNode = currentNode.getChildNodes()[0];
                        recursionGrammarNodes(childNode, start, end);
                    }
                }
            }
        }

        public NFAMap build() {
            int startNode = getNewStatusNum();
            int endNode = getNewStatusNum();
            recursionGrammarNodes(node, startNode, endNode);

            NFAMap nfaMap = new NFAMap();
            nfaMap.nodes = nodeArrayList.toArray(new Node[0]);
            nfaMap.originalRegularExpression = node.getContentForm();
            nfaMap.startNode = String.valueOf(startNode);
            nfaMap.endNode = String.valueOf(endNode);
            int maxStatus = statusCounter--;
            nfaMap.normalNodeList = new String[maxStatus - 2];
            for (int i = 2; i < maxStatus; i++) {
                nfaMap.normalNodeList[i - 2] = String.valueOf(i);
            }
            return nfaMap;
        }
    }

    public static class Node {
        private final String from;
        private final String symbol;
        private final String to;

        Node(String from, String symbol, String to) {
            this.from = from;
            this.symbol = symbol;
            this.to = to;
        }

        @Override
        public String toString() {
            return from + "--" + symbol + "-->" + to;
        }
    }
}
