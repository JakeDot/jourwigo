package net.jakedot.jourwigo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for constructing branching dialogue sequences with normal frames (text and media)
 * followed by a Yes/No or Multiple Choice question leading to choice-specific follow-up dialogue branches.
 */
public class DialogBuilder {

    public enum QuestionType {
        YES_NO,
        MULTIPLE_CHOICE
    }

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

    public DialogBuilder addInitialFrame(String text, String mediaVar) {
        initialFrames.add(new DialogFrame(text, mediaVar));
        return this;
    }

    public List<DialogFrame> getInitialFrames() {
        return Collections.unmodifiableList(initialFrames);
    }

    public DialogBuilder setQuestion(String text, String mediaVar, QuestionType type, List<String> choices) {
        this.question = new Question(text, mediaVar, type, choices);
        return this;
    }

    public Question getQuestion() {
        return question;
    }

    public DialogBuilder addBranchFrame(String choiceLabel, String text, String mediaVar) {
        if (choiceLabel != null && !choiceLabel.isBlank()) {
            String key = choiceLabel.trim();
            branches.computeIfAbsent(key, k -> new ArrayList<>()).add(new DialogFrame(text, mediaVar));
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
     * Creates a default 3-frame dialog sequence with a Yes/No question and standard branches.
     */
    public static DialogBuilder createDefault3FrameDialog() {
        DialogBuilder builder = new DialogBuilder("dialogBranchingDemo");
        builder.addInitialFrame("Welcome traveler! You have arrived at the mystical ancient ruins.", "mediaIntro")
               .addInitialFrame("Legend speaks of a hidden treasure buried beneath these stones.", "mediaRuins")
               .addInitialFrame("Beware, for ancient guardians still watch over this sanctuary.", "mediaGuardian");

        List<String> choices = List.of("Yes", "No");
        builder.setQuestion("Do you wish to enter the ruins and search for the treasure?", "mediaQuestion", QuestionType.YES_NO, choices);

        builder.addBranchFrame("Yes", "You step bravely into the shadows of the ruin...", "mediaEnter")
               .addBranchFrame("Yes", "A glowing chest appears in front of you! You found the treasure!", "mediaTreasure");

        builder.addBranchFrame("No", "You decide to stay safe and turn back.", "mediaTurnBack")
               .addBranchFrame("No", "Perhaps another day adventure will call again.", "mediaGoodbye");

        return builder;
    }

    /**
     * Generates standard Wherigo Lua code for this branching dialogue sequence.
     */
    public String toLuaCode() {
        StringBuilder sb = new StringBuilder();
        String funcName = (name != null && !name.isBlank()) ? name : "dialogSequence";
        sb.append(funcName).append(" = function()\n");

        if (initialFrames.isEmpty()) {
            sb.append(generateQuestionLua("    "));
        } else {
            sb.append(generateFrameChainLua(initialFrames, 0, "    ", true));
        }

        sb.append("end\n");
        return sb.toString();
    }

    private String generateFrameChainLua(List<DialogFrame> frames, int index, String indent, boolean isInitialChain) {
        StringBuilder sb = new StringBuilder();
        DialogFrame frame = frames.get(index);
        sb.append(indent).append("Wherigo.MessageBox({\n");
        sb.append(indent).append("    Text = \"").append(LuaObjectBuilder.escapeLua(frame.getText())).append("\"");
        if (frame.getMediaVar() != null && !frame.getMediaVar().isBlank()) {
            sb.append(",\n").append(indent).append("    Media = ").append(frame.getMediaVar());
        }

        if (index + 1 < frames.size()) {
            sb.append(",\n").append(indent).append("    Callback = function()\n");
            sb.append(generateFrameChainLua(frames, index + 1, indent + "        ", isInitialChain));
            sb.append(indent).append("    end\n");
        } else if (isInitialChain && question != null) {
            sb.append(",\n").append(indent).append("    Callback = function()\n");
            sb.append(generateQuestionLua(indent + "        "));
            sb.append(indent).append("    end\n");
        } else {
            sb.append("\n");
        }

        sb.append(indent).append("})\n");
        return sb.toString();
    }

    private String generateQuestionLua(String indent) {
        StringBuilder sb = new StringBuilder();
        if (question == null) {
            return sb.toString();
        }

        if (question.getType() == QuestionType.YES_NO) {
            List<String> choices = question.getChoices();
            String btn1 = choices.size() > 0 ? choices.get(0) : "Yes";
            String btn2 = choices.size() > 1 ? choices.get(1) : "No";

            sb.append(indent).append("Wherigo.MessageBox({\n");
            sb.append(indent).append("    Text = \"").append(LuaObjectBuilder.escapeLua(question.getText())).append("\"");
            if (question.getMediaVar() != null && !question.getMediaVar().isBlank()) {
                sb.append(",\n").append(indent).append("    Media = ").append(question.getMediaVar());
            }
            sb.append(",\n").append(indent).append("    Buttons = {\"")
              .append(LuaObjectBuilder.escapeLua(btn1)).append("\", \"")
              .append(LuaObjectBuilder.escapeLua(btn2)).append("\"},\n");
            sb.append(indent).append("    Callback = function(action)\n");
            sb.append(indent).append("        if action == \"Button1\" then\n");

            List<DialogFrame> yesFrames = getBranchFrames(btn1);
            if (!yesFrames.isEmpty()) {
                sb.append(generateFrameChainLua(yesFrames, 0, indent + "            ", false));
            } else {
                sb.append(indent).append("            -- End of branch '").append(btn1).append("'\n");
            }

            sb.append(indent).append("        else\n");

            List<DialogFrame> noFrames = getBranchFrames(btn2);
            if (!noFrames.isEmpty()) {
                sb.append(generateFrameChainLua(noFrames, 0, indent + "            ", false));
            } else {
                sb.append(indent).append("            -- End of branch '").append(btn2).append("'\n");
            }

            sb.append(indent).append("        end\n");
            sb.append(indent).append("    end\n");
            sb.append(indent).append("})\n");
        } else {
            // MULTIPLE_CHOICE question via ZInput
            String inputVar = "input_" + (name != null ? name : "question");
            sb.append(indent).append("local ").append(inputVar).append(" = Wherigo.ZInput(Cartridge)\n");
            sb.append(indent).append(inputVar).append(".Name = \"").append(LuaObjectBuilder.escapeLua(name)).append("\"\n");
            sb.append(indent).append(inputVar).append(".Question = \"").append(LuaObjectBuilder.escapeLua(question.getText())).append("\"\n");
            if (question.getMediaVar() != null && !question.getMediaVar().isBlank()) {
                sb.append(indent).append(inputVar).append(".Media = ").append(question.getMediaVar()).append("\n");
            }
            sb.append(indent).append(inputVar).append(".InputType = \"MultipleChoice\"\n");
            sb.append(indent).append(inputVar).append(".Choices = {");
            List<String> choices = question.getChoices();
            for (int i = 0; i < choices.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(LuaObjectBuilder.escapeLua(choices.get(i))).append("\"");
            }
            sb.append("}\n");

            sb.append(indent).append(inputVar).append(".OnGetInput = function(self, input)\n");
            for (int i = 0; i < choices.size(); i++) {
                String choice = choices.get(i);
                if (i == 0) {
                    sb.append(indent).append("    if input == \"").append(LuaObjectBuilder.escapeLua(choice)).append("\" then\n");
                } else {
                    sb.append(indent).append("    elseif input == \"").append(LuaObjectBuilder.escapeLua(choice)).append("\" then\n");
                }
                List<DialogFrame> branchFrames = getBranchFrames(choice);
                if (!branchFrames.isEmpty()) {
                    sb.append(generateFrameChainLua(branchFrames, 0, indent + "        ", false));
                } else {
                    sb.append(indent).append("        -- End of branch '").append(choice).append("'\n");
                }
            }
            sb.append(indent).append("    end\n");
            sb.append(indent).append("end\n");

            sb.append(indent).append("Wherigo.GetInput(").append(inputVar).append(")\n");
        }

        return sb.toString();
    }
}
