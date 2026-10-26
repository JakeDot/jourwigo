package net.jakedot.jourwigo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for constructing branching and merging dialogue graphs with normal frames (text and media)
 * and decision questions leading to choice-specific targets or common merged follow-up nodes.
 */
public class DialogBuilder {

    public enum QuestionType {
        YES_NO,
        MULTIPLE_CHOICE
    }

    public enum NodeType {
        FRAME,
        QUESTION
    }

    /**
     * Base interface/class for a node in the dialogue graph.
     */
    public abstract static class DialogNode {
        private final String id;

        public DialogNode(String id) {
            this.id = id != null && !id.isBlank() ? id.trim() : "node_" + System.nanoTime();
        }

        public String getId() {
            return id;
        }

        public abstract NodeType getNodeType();
    }

    /**
     * Represents a single dialogue frame node.
     */
    public static class FrameNode extends DialogNode {
        private String text;
        private String mediaVar;
        private String nextNodeId;

        public FrameNode(String id, String text, String mediaVar) {
            this(id, text, mediaVar, null);
        }

        public FrameNode(String id, String text, String mediaVar, String nextNodeId) {
            super(id);
            this.text = text != null ? text : "";
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
            this.nextNodeId = nextNodeId != null && !nextNodeId.isBlank() ? nextNodeId.trim() : null;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.FRAME;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text != null ? text : "";
        }

        public String getMediaVar() {
            return mediaVar;
        }

        public void setMediaVar(String mediaVar) {
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
        }

        public String getNextNodeId() {
            return nextNodeId;
        }

        public void setNextNodeId(String nextNodeId) {
            this.nextNodeId = nextNodeId != null && !nextNodeId.isBlank() ? nextNodeId.trim() : null;
        }
    }

    /**
     * Represents a decision question node in the dialogue graph.
     */
    public static class QuestionNode extends DialogNode {
        private String text;
        private String mediaVar;
        private QuestionType questionType;
        private final List<String> choices = new ArrayList<>();
        private final Map<String, String> choiceTargets = new LinkedHashMap<>();

        public QuestionNode(String id, String text, String mediaVar, QuestionType questionType, List<String> choices) {
            super(id);
            this.text = text != null ? text : "";
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
            this.questionType = questionType != null ? questionType : QuestionType.YES_NO;
            if (choices != null && !choices.isEmpty()) {
                for (String c : choices) {
                    if (c != null && !c.isBlank()) {
                        this.choices.add(c.trim());
                    }
                }
            }
            if (this.choices.isEmpty()) {
                if (this.questionType == QuestionType.YES_NO) {
                    this.choices.add("Yes");
                    this.choices.add("No");
                } else {
                    this.choices.add("Option 1");
                    this.choices.add("Option 2");
                }
            }
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.QUESTION;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text != null ? text : "";
        }

        public String getMediaVar() {
            return mediaVar;
        }

        public void setMediaVar(String mediaVar) {
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
        }

        public QuestionType getQuestionType() {
            return questionType;
        }

        public void setQuestionType(QuestionType questionType) {
            this.questionType = questionType != null ? questionType : QuestionType.YES_NO;
        }

        public List<String> getChoices() {
            return Collections.unmodifiableList(choices);
        }

        public void setChoiceTarget(String choiceLabel, String targetNodeId) {
            if (choiceLabel != null && !choiceLabel.isBlank()) {
                if (targetNodeId != null && !targetNodeId.isBlank()) {
                    choiceTargets.put(choiceLabel.trim(), targetNodeId.trim());
                } else {
                    choiceTargets.remove(choiceLabel.trim());
                }
            }
        }

        public String getChoiceTarget(String choiceLabel) {
            if (choiceLabel == null) return null;
            return choiceTargets.get(choiceLabel.trim());
        }

        public Map<String, String> getChoiceTargets() {
            return Collections.unmodifiableMap(choiceTargets);
        }
    }

    // Standard frame/question classes maintained for backward compatibility in helper signatures
    public static class DialogFrame {
        private String text;
        private String mediaVar;

        public DialogFrame(String text, String mediaVar) {
            this.text = text != null ? text : "";
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text != null ? text : "";
        }

        public String getMediaVar() {
            return mediaVar;
        }

        public void setMediaVar(String mediaVar) {
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DialogFrame frame = (DialogFrame) o;
            return Objects.equals(text, frame.text) && Objects.equals(mediaVar, frame.mediaVar);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, mediaVar);
        }
    }

    public static class Question {
        private String text;
        private String mediaVar;
        private QuestionType type;
        private final List<String> choices = new ArrayList<>();

        public Question(String text, String mediaVar, QuestionType type, List<String> choices) {
            this.text = text != null ? text : "";
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
            this.type = type != null ? type : QuestionType.YES_NO;
            if (choices != null && !choices.isEmpty()) {
                for (String c : choices) {
                    if (c != null && !c.isBlank()) {
                        this.choices.add(c.trim());
                    }
                }
            }
            if (this.choices.isEmpty()) {
                if (this.type == QuestionType.YES_NO) {
                    this.choices.add("Yes");
                    this.choices.add("No");
                } else {
                    this.choices.add("Option 1");
                    this.choices.add("Option 2");
                }
            }
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text != null ? text : "";
        }

        public String getMediaVar() {
            return mediaVar;
        }

        public void setMediaVar(String mediaVar) {
            this.mediaVar = mediaVar != null ? mediaVar.trim() : null;
        }

        public QuestionType getType() {
            return type;
        }

        public void setType(QuestionType type) {
            this.type = type != null ? type : QuestionType.YES_NO;
        }

        public List<String> getChoices() {
            return Collections.unmodifiableList(choices);
        }
    }

    private String name;
    private String resultVariable;
    private String startNodeId;
    private final Map<String, DialogNode> nodes = new LinkedHashMap<>();

    // High-level tree structures kept in sync for simple tree API calls
    private final List<DialogFrame> initialFrames = new ArrayList<>();
    private Question question;
    private final Map<String, List<DialogFrame>> branches = new LinkedHashMap<>();

    public DialogBuilder() {
        this("dialogSequence");
    }

    public DialogBuilder(String name) {
        this.name = name != null && !name.isBlank() ? name.trim() : "dialogSequence";
    }

    public String getName() {
        return name;
    }

    public DialogBuilder setName(String name) {
        this.name = name != null && !name.isBlank() ? name.trim() : "dialogSequence";
        return this;
    }

    public String getResultVariable() {
        return resultVariable;
    }

    public DialogBuilder setResultVariable(String resultVariable) {
        this.resultVariable = resultVariable != null && !resultVariable.isBlank() ? resultVariable.trim() : null;
        return this;
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public DialogBuilder setStartNodeId(String startNodeId) {
        this.startNodeId = startNodeId != null && !startNodeId.isBlank() ? startNodeId.trim() : null;
        return this;
    }

    public DialogBuilder addNode(DialogNode node) {
        if (node != null) {
            nodes.put(node.getId(), node);
            if (startNodeId == null) {
                startNodeId = node.getId();
            }
        }
        return this;
    }

    public DialogNode getNode(String id) {
        return id != null ? nodes.get(id.trim()) : null;
    }

    public Map<String, DialogNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    // High-level backward compatible Tree API methods
    public DialogBuilder addInitialFrame(String text, String mediaVar) {
        initialFrames.add(new DialogFrame(text, mediaVar));
        syncTreeToGraph();
        return this;
    }

    public List<DialogFrame> getInitialFrames() {
        return Collections.unmodifiableList(initialFrames);
    }

    public DialogBuilder setQuestion(String text, String mediaVar, QuestionType type, List<String> choices) {
        this.question = new Question(text, mediaVar, type, choices);
        syncTreeToGraph();
        return this;
    }

    public Question getQuestion() {
        return question;
    }

    public DialogBuilder addBranchFrame(String choiceLabel, String text, String mediaVar) {
        if (choiceLabel != null && !choiceLabel.isBlank()) {
            String key = choiceLabel.trim();
            branches.computeIfAbsent(key, k -> new ArrayList<>()).add(new DialogFrame(text, mediaVar));
            syncTreeToGraph();
        }
        return this;
    }

    public List<DialogFrame> getBranchFrames(String choiceLabel) {
        if (choiceLabel == null) return Collections.emptyList();
        List<DialogFrame> frames = branches.get(choiceLabel.trim());
        return frames != null ? Collections.unmodifiableList(frames) : Collections.emptyList();
    }

    public Map<String, List<DialogFrame>> getBranches() {
        return Collections.unmodifiableMap(branches);
    }

    /**
     * Synchronizes high-level tree properties into graph node structures (`nodes`).
     */
    private void syncTreeToGraph() {
        nodes.clear();
        String prevNodeId = null;

        // Build initial frames as chain
        for (int i = 0; i < initialFrames.size(); i++) {
            String id = "initial_frame_" + (i + 1);
            DialogFrame frame = initialFrames.get(i);
            FrameNode fn = new FrameNode(id, frame.getText(), frame.getMediaVar());
            if (prevNodeId != null && nodes.get(prevNodeId) instanceof FrameNode prevFn) {
                prevFn.setNextNodeId(id);
            }
            nodes.put(id, fn);
            if (i == 0) {
                startNodeId = id;
            }
            prevNodeId = id;
        }

        // Question node
        if (question != null) {
            String qId = "question_node";
            QuestionNode qn = new QuestionNode(qId, question.getText(), question.getMediaVar(), question.getType(), question.getChoices());
            if (prevNodeId != null && nodes.get(prevNodeId) instanceof FrameNode prevFn) {
                prevFn.setNextNodeId(qId);
            }
            nodes.put(qId, qn);
            if (startNodeId == null) {
                startNodeId = qId;
            }

            for (String choice : question.getChoices()) {
                List<DialogFrame> bFrames = branches.get(choice);
                if (bFrames != null && !bFrames.isEmpty()) {
                    String branchPrevId = null;
                    for (int j = 0; j < bFrames.size(); j++) {
                        String bId = "branch_" + choice.replaceAll("[^a-zA-Z0-9_]", "_") + "_" + (j + 1);
                        DialogFrame bf = bFrames.get(j);
                        FrameNode bfn = new FrameNode(bId, bf.getText(), bf.getMediaVar());
                        if (j == 0) {
                            qn.setChoiceTarget(choice, bId);
                        } else if (branchPrevId != null && nodes.get(branchPrevId) instanceof FrameNode prevBfn) {
                            prevBfn.setNextNodeId(bId);
                        }
                        nodes.put(bId, bfn);
                        branchPrevId = bId;
                    }
                }
            }
        }
    }

    /**
     * Creates a default 3-frame dialog sequence with a Yes/No question and standard branches.
     */
    public static DialogBuilder createDefault3FrameDialog() {
        DialogBuilder builder = new DialogBuilder("dialogBranchingDemo");
        builder.addInitialFrame("Welcome traveler! You have arrived at the mystical ancient ruins.", "mediaIntro")
               .addInitialFrame("Legend speaks of a hidden treasure buried beneath these stones.", "mediaRuins")
               .addInitialFrame("Beware, for ancient guardians still watch over this sanctuary.", "mediaGuardian");

        List<String> choices = List.of("Yes", "No");
        builder.setQuestion("Do you wish to enter the ruins and search for the treasure?", "mediaQuestion", QuestionType.YES_NO, choices);
        builder.setResultVariable("varRuinsAccepted");

        builder.addBranchFrame("Yes", "You step bravely into the shadows of the ruin...", "mediaEnter")
               .addBranchFrame("Yes", "A glowing chest appears in front of you! You found the treasure!", "mediaTreasure");

        builder.addBranchFrame("No", "You decide to stay safe and turn back.", "mediaTurnBack")
               .addBranchFrame("No", "Perhaps another day adventure will call again.", "mediaGoodbye");

        return builder;
    }

    /**
     * Creates a dialogue graph demonstrating branch merging.
     * Both choice "Explore Path" and choice "Rest Here" merge back into a common follow-up node.
     */
    public static DialogBuilder createMergingGraphDialog() {
        DialogBuilder builder = new DialogBuilder("dialogMergeDemo");
        builder.setResultVariable("varChoiceMade");

        FrameNode start = new FrameNode("node_start", "You stand at a crossroad in the forest.", "mediaCrossroads", "node_question");
        QuestionNode q = new QuestionNode("node_question", "What do you do?", "mediaQuestion", QuestionType.YES_NO, List.of("Explore Path", "Rest Here"));

        FrameNode explore = new FrameNode("node_explore", "You explore down the narrow path, finding ancient stone markers.", "mediaPath", "node_merged");
        FrameNode rest = new FrameNode("node_rest", "You sit down by the old oak tree and catch your breath.", "mediaRest", "node_merged");
        FrameNode merged = new FrameNode("node_merged", "Regardless of your choice, night approaches and you reach the village tavern together.", "mediaTavern", null);

        q.setChoiceTarget("Explore Path", "node_explore");
        q.setChoiceTarget("Rest Here", "node_rest");

        builder.addNode(start)
               .addNode(q)
               .addNode(explore)
               .addNode(rest)
               .addNode(merged);

        return builder;
    }

    /**
     * Generates Lua code for this dialogue graph (supporting arbitrary branching, merging, and cycles).
     */
    public String toLuaCode() {
        StringBuilder sb = new StringBuilder();
        String funcName = (name != null && !name.isBlank()) ? name : "dialogSequence";

        sb.append(funcName).append(" = function()\n");

        if (nodes.isEmpty()) {
            sb.append("end\n");
            return sb.toString();
        }

        // Sanitize Lua identifiers for node local functions
        Map<String, String> luaFuncs = new LinkedHashMap<>();
        int counter = 1;
        for (String nodeId : nodes.keySet()) {
            luaFuncs.put(nodeId, "step_" + counter++);
        }

        // 1. Declare local node functions inside dialogue function scope
        sb.append("    local ");
        List<String> funcNamesList = new ArrayList<>(luaFuncs.values());
        for (int i = 0; i < funcNamesList.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(funcNamesList.get(i));
        }
        sb.append("\n\n");

        // 2. Define node functions
        for (Map.Entry<String, DialogNode> entry : nodes.entrySet()) {
            String nodeId = entry.getKey();
            DialogNode node = entry.getValue();
            String luaFuncName = luaFuncs.get(nodeId);

            sb.append("    ").append(luaFuncName).append(" = function()\n");

            if (node instanceof FrameNode fn) {
                sb.append("        Wherigo.MessageBox({\n");
                sb.append("            Text = \"").append(LuaObjectBuilder.escapeLua(fn.getText())).append("\"");
                if (fn.getMediaVar() != null && !fn.getMediaVar().isBlank()) {
                    sb.append(",\n            Media = ").append(fn.getMediaVar());
                }
                if (fn.getNextNodeId() != null && luaFuncs.containsKey(fn.getNextNodeId())) {
                    sb.append(",\n            Callback = function()\n");
                    sb.append("                ").append(luaFuncs.get(fn.getNextNodeId())).append("()\n");
                    sb.append("            end\n");
                } else {
                    sb.append("\n");
                }
                sb.append("        })\n");
            } else if (node instanceof QuestionNode qn) {
                if (qn.getQuestionType() == QuestionType.YES_NO) {
                    List<String> choices = qn.getChoices();
                    String btn1 = choices.size() > 0 ? choices.get(0) : "Yes";
                    String btn2 = choices.size() > 1 ? choices.get(1) : "No";

                    sb.append("        Wherigo.MessageBox({\n");
                    sb.append("            Text = \"").append(LuaObjectBuilder.escapeLua(qn.getText())).append("\"");
                    if (qn.getMediaVar() != null && !qn.getMediaVar().isBlank()) {
                        sb.append(",\n            Media = ").append(qn.getMediaVar());
                    }
                    sb.append(",\n            Buttons = {\"")
                      .append(LuaObjectBuilder.escapeLua(btn1)).append("\", \"")
                      .append(LuaObjectBuilder.escapeLua(btn2)).append("\"},\n");
                    sb.append("            Callback = function(action)\n");
                    sb.append("                if action == \"Button1\" then\n");
                    if (resultVariable != null && !resultVariable.isBlank()) {
                        sb.append("                    ").append(resultVariable).append(" = \"").append(LuaObjectBuilder.escapeLua(btn1)).append("\"\n");
                    }
                    String target1 = qn.getChoiceTarget(btn1);
                    if (target1 != null && luaFuncs.containsKey(target1)) {
                        sb.append("                    ").append(luaFuncs.get(target1)).append("()\n");
                    }

                    sb.append("                else\n");
                    if (resultVariable != null && !resultVariable.isBlank()) {
                        sb.append("                    ").append(resultVariable).append(" = \"").append(LuaObjectBuilder.escapeLua(btn2)).append("\"\n");
                    }
                    String target2 = qn.getChoiceTarget(btn2);
                    if (target2 != null && luaFuncs.containsKey(target2)) {
                        sb.append("                    ").append(luaFuncs.get(target2)).append("()\n");
                    }
                    sb.append("                end\n");
                    sb.append("            end\n");
                    sb.append("        })\n");
                } else {
                    // MULTIPLE_CHOICE question via ZInput
                    String inputVar = "input_" + (nodeId != null ? nodeId.replaceAll("[^a-zA-Z0-9_]", "_") : "question");
                    sb.append("        local ").append(inputVar).append(" = Wherigo.ZInput(Cartridge)\n");
                    sb.append("        ").append(inputVar).append(".Name = \"").append(LuaObjectBuilder.escapeLua(name)).append("\"\n");
                    sb.append("        ").append(inputVar).append(".Question = \"").append(LuaObjectBuilder.escapeLua(qn.getText())).append("\"\n");
                    if (qn.getMediaVar() != null && !qn.getMediaVar().isBlank()) {
                        sb.append("        ").append(inputVar).append(".Media = ").append(qn.getMediaVar()).append("\n");
                    }
                    sb.append("        ").append(inputVar).append(".InputType = \"MultipleChoice\"\n");
                    sb.append("        ").append(inputVar).append(".Choices = {");
                    List<String> choices = qn.getChoices();
                    for (int i = 0; i < choices.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append("\"").append(LuaObjectBuilder.escapeLua(choices.get(i))).append("\"");
                    }
                    sb.append("}\n");

                    sb.append("        ").append(inputVar).append(".OnGetInput = function(self, input)\n");
                    if (resultVariable != null && !resultVariable.isBlank()) {
                        sb.append("            ").append(resultVariable).append(" = input\n");
                    }
                    for (int i = 0; i < choices.size(); i++) {
                        String choice = choices.get(i);
                        if (i == 0) {
                            sb.append("            if input == \"").append(LuaObjectBuilder.escapeLua(choice)).append("\" then\n");
                        } else {
                            sb.append("            elseif input == \"").append(LuaObjectBuilder.escapeLua(choice)).append("\" then\n");
                        }
                        String target = qn.getChoiceTarget(choice);
                        if (target != null && luaFuncs.containsKey(target)) {
                            sb.append("                ").append(luaFuncs.get(target)).append("()\n");
                        }
                    }
                    sb.append("            end\n");
                    sb.append("        end\n");

                    sb.append("        Wherigo.GetInput(").append(inputVar).append(")\n");
                }
            }

            sb.append("    end\n\n");
        }

        // 3. Start graph execution at startNodeId
        String initialStep = (startNodeId != null && luaFuncs.containsKey(startNodeId)) ? luaFuncs.get(startNodeId) : funcNamesList.get(0);
        sb.append("    ").append(initialStep).append("()\n");

        sb.append("end\n");
        return sb.toString();
    }
}
