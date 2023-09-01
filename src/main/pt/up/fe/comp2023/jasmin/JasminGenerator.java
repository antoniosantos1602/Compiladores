package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map;

public class JasminGenerator {
    private final ClassUnit classUnit;
    private int numcond=0;

    private int instrCurrStackSize;

    private int instrMaxStackSize;



    public JasminGenerator(ClassUnit classUnit){
        this.classUnit = classUnit;


        this.instrCurrStackSize = 0;
        this.instrMaxStackSize = 0;

    }

    /*
        .class public HelloWorld
        .super java/lang/Object
        .method public <init>()V
            aload_0
            invokenonvirtual java/lang/Object/<init>()V
            return
        .end method
     */


    public String convertClass(){
        StringBuilder code = new StringBuilder();

        //.class
        code.append(this.buildClassDirective());

        // .super
        code.append(this.buildSuperClassDirective());

        code.append(this.buildClassFields());

        code.append(this.buildMethods());

        return code.toString();
    }

    private String buildClassDirective() {
        return ".class public " + this.classUnit.getClassName() + "\n";
    }

    private String buildSuperClassDirective(){
        if(this.classUnit.getSuperClass() == null)
            return ".super java/lang/Object\n";
        return ".super " + this.classUnit.getSuperClass() + "\n";
    }

    private String buildClassFields()
    {
        StringBuilder code = new StringBuilder();
        for (Field field : classUnit.getFields())
        {
            code.append(".field ").append(field.getFieldName()).append(" ").append(this.convertType(field.getFieldType())).append("\n");
        }
        return code.toString();
    }

    //////////////////////////
    // gera todos os métodos na classe
    private String buildMethods(){
        StringBuilder code = new StringBuilder();
        for (Method method : classUnit.getMethods()) {
            this.instrCurrStackSize = 0;
            this.instrMaxStackSize = 0;
            code.append(this.buildMethodHeader(method));
            String instructions = this.buildMethodInstructions(method);
            if (!method.isConstructMethod()) {
                code.append(this.dealWithLocalLimit(method));
                code.append(this.dealWithStackLimit(method));
                code.append(instructions);//instructs
            }
        }
        return code.toString();
    }


    // gerar o cabeçalho de cada método
    private String buildMethodHeader(Method method){
        // se o método for o construtor gera o header padrao
        // verifica se o nome do método é igual ao nome da classe
        if(method.isConstructMethod()) {
            String classSuper = "java/lang/Object";

            if (classUnit.getSuperClass() != null)
                classSuper = classUnit.getSuperClass();

            return "\n.method public <init>()V\naload_0\ninvokespecial " + classSuper +  "/<init>()V\nreturn\n.end method\n";
        }
        // se o método não for o construtor gera o header que inclui o modificador de acesso, o nome do método, os parametros e o tipo de retorno
        StringBuilder result = new StringBuilder("\n.method").append(" ").append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");

        if(method.isStaticMethod())
            result.append("static ");
        if(method.isFinalMethod())
            result.append("final ");


        result.append(method.getMethodName()).append("(");

        for(Element element : method.getParams())
            result.append(convertType(element.getType()));

        result.append(")");
        result.append(this.convertType(method.getReturnType())).append("\n");

        return result.toString();
    }

    // se o metodo nao for um construtor a funçao tambem adiciona limites ao metodo


    private String dealWithLocalLimit(Method method) {
        Set<Integer> virtualRegs = new TreeSet<>();
        virtualRegs.add(0);
        StringBuilder string = new StringBuilder();

        for (Descriptor descriptor : method.getVarTable().values()) {
            virtualRegs.add(descriptor.getVirtualReg());
        }
        string.append(".limit locals ").append(virtualRegs.size()).append("\n");
        return string.toString();
    }

    private String dealWithStackLimit(Method method) {
        StringBuilder string = new StringBuilder();
        string.append(".limit stack ").append(instrMaxStackSize).append("\n");
        return string.toString();
    }


    // gera todas as instruçoes dentro de um método(se houver)
    // verificar caso a função seja uma chamada ao método de um tipo de retorno diferente de void o valor retornado
    // seja removido da pilha apos a chamada


    private String buildMethodInstructions(Method method){
        StringBuilder string = new StringBuilder();
        method.getVarTable();
        for (Instruction instruction : method.getInstructions()) {
            string.append(buildInstruction(instruction, method.getVarTable(), method.getLabels()));
            if (instruction instanceof CallInstruction && ((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
                string.append("pop\n");
                this.decrementStackCounter(1);
            }
        }

        string.append("\n.end method\n");
        return string.toString();
    }

    // criar método para lidar com as diferentes instruções
    private String buildInstruction(Instruction instruction, HashMap<String, Descriptor> table, HashMap<String, Instruction> methodLabels){
        StringBuilder string = new StringBuilder();
        for (Map.Entry<String, Instruction> entry : methodLabels.entrySet()) {
            if (entry.getValue().equals(instruction)){
                string.append(entry.getKey()).append(":\n");
            }
        }

        switch (instruction.getInstType()) {
            case ASSIGN:
                return string.append(AssignmentInstruction((AssignInstruction) instruction, table)).toString();
            case CALL:
                return string.append(CallInstruction((CallInstruction) instruction, table)).toString();

            case GOTO:
                return string.append(getCode((GotoInstruction) instruction, table)).toString();

            case BRANCH:
                return string.append(buildCondBranchInstruction((CondBranchInstruction) instruction, table)).toString();

            case RETURN:
                return string.append(ReturnInstruction((ReturnInstruction) instruction, table)).toString();

            case PUTFIELD:
                return string.append(dealWithPutFieldInstruction((PutFieldInstruction) instruction, table)).toString();

            case GETFIELD:
                return string.append(dealWithGetFieldInstruction((GetFieldInstruction) instruction, table)).toString();

            case UNARYOPER:
                return "TODO";

            case BINARYOPER:
                return string.append(dealWithBinaryOpInstruction((BinaryOpInstruction) instruction, table)).toString();

            case NOPER:
                return string.append(SingleOpInstruction((SingleOpInstruction) instruction, table)).toString();

            default:
                return "Error";
        }
    }

    private String buildCondBranchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> table) {
        StringBuilder jasminCode = new StringBuilder();

        Instruction condition = instruction.getCondition();

        String branchop;

        if (condition.getInstType() == InstructionType.UNARYOPER) {
            UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) condition;
            if (unaryOpInstruction.getOperation().getOpType() == OperationType.NOTB) {
                jasminCode.append(this.loadElement(unaryOpInstruction.getOperand(), table));
                branchop = "ifeq";
            } else {
                jasminCode.append("Error unaryOperation\n");
                jasminCode.append(this.buildInstruction(condition, table, new HashMap<>()));
                branchop = "ifne";
            }
        } else if (condition.getInstType() == InstructionType.BINARYOPER) {
            BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) condition;
            OperationType opType = binaryOpInstruction.getOperation().getOpType();

            Element left = binaryOpInstruction.getLeftOperand();
            Element right = binaryOpInstruction.getRightOperand();

            boolean isLiteralComparison = false;

            if (opType == OperationType.LTH) {
                branchop = "if_icmplt";

                if (left instanceof LiteralElement) {
                    String literal = ((LiteralElement) left).getLiteral();
                    jasminCode.append(this.loadElement(right, table));
                    jasminCode.append("\tldc ").append(literal).append("\n");
                    branchop = "if_icmpge";
                    isLiteralComparison = true;
                } else if (right instanceof LiteralElement) {
                    String literal = ((LiteralElement) right).getLiteral();
                    jasminCode.append(this.loadElement(left, table));
                    jasminCode.append("\tldc ").append(literal).append("\n");
                    branchop = "if_icmplt";
                    isLiteralComparison = true;
                }
            } else if (opType == OperationType.GTE) {
                branchop = "if_icmpge";

                if (left instanceof LiteralElement) {
                    String literal = ((LiteralElement) left).getLiteral();
                    jasminCode.append(this.loadElement(right, table));
                    jasminCode.append("\tldc ").append(literal).append("\n");
                    branchop = "if_icmplt";
                    isLiteralComparison = true;
                } else if (right instanceof LiteralElement) {
                    String literal = ((LiteralElement) right).getLiteral();
                    jasminCode.append(this.loadElement(left, table));
                    jasminCode.append("\tldc ").append(literal).append("\n");
                    branchop = "if_icmpge";
                    isLiteralComparison = true;
                }
            } else if (opType == OperationType.ANDB) {
                jasminCode.append(this.buildInstruction(condition, table, new HashMap<>()));
                branchop = "ifne";
            } else {
                jasminCode.append("Error binaryOperation\n" + binaryOpInstruction.getOperation().getOpType() + "\n");
                jasminCode.append(this.buildInstruction(condition, table, new HashMap<>()));
                branchop = "ifne";
            }

            if (!isLiteralComparison) {
                jasminCode.append(this.loadElement(left, table))
                        .append(this.loadElement(right, table));
            }

            jasminCode.append("\t").append(branchop).append(" ").append(instruction.getLabel()).append("\n");
        } else {
            jasminCode.append(this.buildInstruction(condition, table, new HashMap<>()));
            branchop = "ifne";
            jasminCode.append("\t").append(branchop).append(" ").append(instruction.getLabel()).append("\n");
        }

        if (branchop.equals("if_icmplt")) {
            this.decrementStackCounter(2);
        } else {
            this.decrementStackCounter(1);
        }

        return jasminCode.toString();
    }

    private String AssignmentInstruction(AssignInstruction instruction, HashMap<String, Descriptor> table){
        StringBuilder result = new StringBuilder();

        // carrega o operando de destino
        Operand destination = (Operand) instruction.getDest();
        if(destination instanceof ArrayOperand arrayOperation) {
            // se o destino for um array, carrega o array e o índice
            result.append("aload").append(this.getVirtualReg(destination.getName(), table)).append("\n");
            this.incrementStackCounter(1);
            result.append(loadElement(arrayOperation.getIndexOperands().get(0), table));
        }

        // carrega o valor a ser atribuído
        result.append(buildInstruction(instruction.getRhs(), table, new HashMap<String, Instruction>()));

        // armazena o valor no operando de destino
        if(!(destination.getType().getTypeOfElement().equals(ElementType.OBJECTREF) && instruction.getRhs() instanceof CallInstruction)){
            result.append(storeElement(destination, table));
        }

        return result.toString();
    }



    private String CallInstruction(CallInstruction instruction, HashMap<String, Descriptor> table){
        StringBuilder result = new StringBuilder();
        CallType call = instruction.getInvocationType();

        switch (instruction.getInvocationType()){
            case invokevirtual:
                result.append(this.dealWithInvoke(instruction, table, call, ((ClassType)instruction.getFirstArg().getType()).getName()));
                break;
            case invokespecial:
                result.append(this.dealWithInvoke(instruction, table, call, ((ClassType)instruction.getFirstArg().getType()).getName()));
                break;
            case invokestatic:
                result.append(this.dealWithInvoke(instruction, table, call, ((Operand) instruction.getFirstArg()).getName()));
                break;
            case NEW:
                result.append(this.dealWithNewObject(instruction, table));
                break;
            case arraylength:
                result.append(this.loadElement(instruction.getFirstArg(), table));
                result.append("arraylength\n");
                break;
        }
        return result.toString();
    }

    //
    private String dealWithInvoke(CallInstruction instruction, HashMap<String, Descriptor> table, CallType call, String classN){
        StringBuilder result = new StringBuilder();


        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral();
        String params = "";

        if(!methodName.equals("\"<init>\"")){
            result.append(this.loadElement(instruction.getFirstArg(), table));
        }

        int nParams = 0;

        // load arguments
        for(Element element : instruction.getListOfOperands()){
            result.append(this.loadElement(element, table));
            params += this.convertType(element.getType());
            nParams++;
        }

        if (!instruction.getInvocationType().equals(CallType.invokestatic)) {
            nParams += 1;
        }

        this.decrementStackCounter(nParams);

        if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
            this.incrementStackCounter(1);
        }


        result.append(call.name())
                .append(" ")
                .append(this.getOjectClassName(classN))
                .append("/")
                .append(methodName.replace("\"",""))
                .append("(")
                .append(params)
                .append(")")
                .append(this.convertType(instruction.getReturnType()))
                .append("\n");

        if (methodName.equals("\"<init>\"") && !classN.equals("this")) {
            result.append(this.storeElement((Operand) instruction.getFirstArg(), table));
        }

        return result.toString();
    }

    private String dealWithNewObject(CallInstruction instruction, HashMap<String, Descriptor> table){
        Element elem = instruction.getFirstArg();
        String string = "";

        if(elem.getType().getTypeOfElement().equals(ElementType.ARRAYREF)){
            string += this.loadElement(instruction.getListOfOperands().get(0), table);
            string += "newarray int\n";
        }

        if(elem.getType().getTypeOfElement().equals(ElementType.OBJECTREF)) {
            this.incrementStackCounter(1);
            string += "new " + this.getOjectClassName(((Operand) elem).getName()) + "\ndup\n";
        }
        return string;
    }

    private String getCode(GotoInstruction instruction, HashMap<String, Descriptor> table) {
        return "goto " + instruction.getLabel() + "\n";
    }

    private String ReturnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> table){
        if(!instruction.hasReturnValue()) return "return";
        String string = "";

        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case VOID:
                string = "return";
                break;
            case INT32:
            case BOOLEAN:
                string = loadElement(instruction.getOperand(), table);
                this.decrementStackCounter(1);
                string += "ireturn";
                break;
            case ARRAYREF:
            case OBJECTREF:
                string = loadElement(instruction.getOperand(), table);
                this.decrementStackCounter(1);
                string += "areturn";
                break;
            default:
                break;
        }

        return string;
    }
    private String dealWithPutFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> table){
        String string = "";
        Operand object = (Operand) instruction.getFirstOperand();
        Operand variable = (Operand) instruction.getSecondOperand();
        Element value = instruction.getThirdOperand();

        string += this.loadElement(object, table);
        string += this.loadElement(value, table);

        this.decrementStackCounter(2);

        return string + "putfield " + classUnit.getClassName() + "/" + variable.getName() + " " + convertType(variable.getType()) + "\n";
    }

    private String dealWithGetFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> table){
        String string = "";
        Operand object = (Operand) instruction.getFirstOperand();
        Operand variable = (Operand) instruction.getSecondOperand();

        string += this.loadElement(object, table);

        return string + "getfield " + classUnit.getClassName() + "/" + variable.getName() + " " + convertType(variable.getType()) + "\n";
    }

    private String dealWithBinaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> table){
        StringBuilder result = new StringBuilder();
        result.append(this.loadElement(instruction.getLeftOperand(), table)); //leftOp
        result.append(this.loadElement(instruction.getRightOperand(), table)); //rightOp

        switch (instruction.getOperation().getOpType()){
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                return this.IntOperation(instruction, table);
            case LTH:
                StringBuilder code = new StringBuilder();
                code.append(this.loadElement(instruction.getLeftOperand(), table));
                code.append(this.loadElement(instruction.getRightOperand(), table));
                code.append("if_icmplt ").append(this.getTrueLabel()).append("\n");
                code.append("iconst_0\n");
                code.append("goto ").append(this.getEndIfLabel()).append("\n");
                code.append(this.getTrueLabel()).append(":\n");
                code.append("iconst_1\n");
                code.append(this.getEndIfLabel()).append(":\n");
            case GTE:
                String leftOp = loadElement(instruction.getLeftOperand(), table);
                String rightOp = loadElement(instruction.getRightOperand(), table);

                result.append(leftOp)
                        .append(rightOp)
                        .append("if_icmpge ").append(this.getTrueLabel()).append("\n")
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");
                break;
            case ANDB:
            case NOTB:
                return this.dealWithBooleanOperation(instruction, table);
            default:
                return "Error in BinaryOpInstruction";
        }
        return result.toString();
    }

    private String IntOperation(BinaryOpInstruction instruction, HashMap<String, Descriptor> table){
        StringBuilder result = new StringBuilder();
        result.append(this.loadElement(instruction.getLeftOperand(), table)); //leftOp
        result.append(this.loadElement(instruction.getRightOperand(), table)); //rightOp

        switch (instruction.getOperation().getOpType()){
            case ADD:
                 result.append("iadd\n");
                break;
            case SUB:
                result.append("isub\n");
                break;
            case MUL:
                result.append("imul\n");
                break;
            case DIV:
                result.append("idiv\n");
                break;
            default:
                return "Error\n";
        }
        this.decrementStackCounter(1);
        return result.toString();
    }


    private String dealWithBooleanOperation(BinaryOpInstruction instruction, HashMap<String, Descriptor> table){
        OperationType opt = instruction.getOperation().getOpType();
        StringBuilder string = new StringBuilder();

        switch (opt){
            case LTH:
                StringBuilder code = new StringBuilder();
                code.append(this.loadElement(instruction.getLeftOperand(), table));
                code.append(this.loadElement(instruction.getRightOperand(), table));
                code.append("if_icmplt ").append(this.getTrueLabel()).append("\n");
                code.append("iconst_0\n");
                code.append("goto ").append(this.getEndIfLabel()).append("\n");
                code.append(this.getTrueLabel()).append(":\n");
                code.append("iconst_1\n");
                code.append(this.getEndIfLabel()).append(":\n");
                this.decrementStackCounter(1);

            case GTE: {
                String leftOp = loadElement(instruction.getLeftOperand(), table);
                String rightOp = loadElement(instruction.getRightOperand(), table);

                string.append(leftOp)
                        .append(rightOp)
                        .append("if_icmpge ").append(this.getTrueLabel()).append("\n")
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");
                this.decrementStackCounter(1);
                break;
            }
            case ANDB: {
                String aux = "ifeq " + this.getTrueLabel() + "\n";

                string.append(loadElement(instruction.getLeftOperand(), table)).append(aux);
                this.decrementStackCounter(1);

                string.append(loadElement(instruction.getRightOperand(), table)).append(aux);
                this.decrementStackCounter(1);

                string.append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");
                this.incrementStackCounter(1);

                break;
            }
            case NOTB: {
                String operand = loadElement(instruction.getLeftOperand(), table);

                string.append(operand)
                        .append("ifne ").append(this.getTrueLabel()).append("\n")
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");

                break;
            }
            default:
                return "Error in BooleanOperation";
        }

        this.numcond++;
        return string.toString();
    }

    private String SingleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> table) {
        return loadElement(instruction.getSingleOperand(), table);
    }

    public String convertType(Type type){

        if(type instanceof ArrayType){
            return "[" + convertType(((ArrayType) type).getTypeOfElements());
        }
        if(type.getTypeOfElement().equals(ElementType.OBJECTREF)){
            String objectName = ((ClassType)type).getName();
            return "L" + this.getOjectClassName(objectName) + ";";
        }
        return convertType(type.getTypeOfElement());
    }

    public String convertType(ElementType type) {

        switch (type){
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            default:
                return "Error converting ElementType";
        }
    }



    private String getOjectClassName(String className) {
        if(className.equals(this.classUnit.getClassName())){
            return this.classUnit.getClassName();
        }

        for (String _import : classUnit.getImports()) {
            if (_import.endsWith("." + className)) {
                return _import.replace("\\.", "/");
            }
        }
        return className;
    }

    //
    private String getVirtualReg(String varName, HashMap<String, Descriptor> varTable) {
        int virtualReg = varTable.get(varName).getVirtualReg();
        if (virtualReg > 3) {
            return " " + virtualReg;
        }
        return "_" + virtualReg;
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        if (element instanceof LiteralElement) {
            int n = Integer.parseInt(((LiteralElement) element).getLiteral());
            if (0 <= n && n <= 5) {
                result.append("\ticonst_");
            } else if (-128 <= n && n <= 127) {
                result.append("\tbipush ");
            } else if (-32768 <= n && n <= 32767) {
                result.append("\tsipush ");
            } else {
                result.append("\tldc ");
            }
            result.append(n).append("\n");
            this.incrementStackCounter(1);

        } else if (element instanceof ArrayOperand) {
            ArrayOperand arrayOperand = (ArrayOperand) element;
            result.append("\taload").append(this.getVirtualReg(arrayOperand.getName(), varTable)).append("\n");
            this.incrementStackCounter(1);

            result.append(this.loadElement(arrayOperand.getIndexOperands().get(0), varTable));

            result.append("\tiaload\n");
            this.decrementStackCounter(1);
        } else if (element instanceof Operand) {
            Operand operand = (Operand) element;
            ElementType type = operand.getType().getTypeOfElement();
            switch (type) {
                case THIS:
                    result.append("\taload_0\n");
                    this.incrementStackCounter(1);
                    break;
                case INT32:
                case BOOLEAN:
                    result.append("\tiload").append(this.getVirtualReg(operand.getName(), varTable)).append("\n");
                    this.incrementStackCounter(1);
                    break;
                case OBJECTREF:
                case ARRAYREF:
                case STRING:
                    result.append("\taload").append(this.getVirtualReg(operand.getName(), varTable)).append("\n");
                    this.incrementStackCounter(1);
                    break;
                case CLASS:
                    break;
                case VOID:
                    throw new RuntimeException("loadElement: Unrecognized load instruction for " + type + " element type");
            }
        }

        return result.toString();
    }

    private String storeElement(Operand op, HashMap<String, Descriptor> table){

        if(op instanceof ArrayOperand){
            this.decrementStackCounter(3);
            return "iastore\n";
        }

        switch (op.getType().getTypeOfElement()){
            case INT32, BOOLEAN:
                this.decrementStackCounter(1);
                return String.format("istore%s\n", this.getVirtualReg(op.getName(), table)); // pop and store in local variable
            case OBJECTREF, ARRAYREF, STRING:
                this.decrementStackCounter(1);
                return String.format("astore%s\n", this.getVirtualReg(op.getName(), table)); //pop and store the object reference in local
            default:
                return "Error";
        }
    }


    private void decrementStackCounter(int i) {
        this.instrCurrStackSize -= i;
    }

    private void incrementStackCounter(int i) {
        this.instrCurrStackSize += i;
        if (this.instrCurrStackSize > this.instrMaxStackSize) {
            this.instrMaxStackSize = instrCurrStackSize;
        }
    }


    private String getTrueLabel() {
        return "myTrue" + this.numcond;
    }

    private String getEndIfLabel() {
        return "myEndIf" + this.numcond;
    }
}



