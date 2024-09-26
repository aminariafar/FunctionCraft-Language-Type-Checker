package main.visitor.type;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.*;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.*;
import main.ast.nodes.expression.value.*;
import main.ast.nodes.expression.value.primitive.*;
import main.ast.nodes.statement.*;
import main.ast.type.*;
import main.ast.type.primitiveType.*;
import main.compileError.CompileError;
import main.compileError.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.*;
import main.symbolTable.item.*;
import main.visitor.Visitor;

//import java.lang.foreign.SymbolLookup;
import java.util.*;

public class TypeChecker extends Visitor<Type> {
    public Set<Type> retTypes = new HashSet<>();
    public ArrayList<CompileError> typeErrors = new ArrayList<>();
    @Override
    public Type visit(Program program){
        SymbolTable.root = new SymbolTable();
        SymbolTable.top = new SymbolTable();
        for(FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()){
            FunctionItem functionItem = new FunctionItem(functionDeclaration);
            try {
                SymbolTable.root.put(functionItem);
            }catch (ItemAlreadyExists ignored){}
        }
        for(PatternDeclaration patternDeclaration : program.getPatternDeclarations()){
            PatternItem patternItem = new PatternItem(patternDeclaration);
            try{
                SymbolTable.root.put(patternItem);
            }catch (ItemAlreadyExists ignored){}
        }
        program.getMain().accept(this);

        return null;
    }
    @Override
    public Type visit(FunctionDeclaration functionDeclaration){
        SymbolTable.push(new SymbolTable());
        try {
            FunctionItem functionItem = (FunctionItem) SymbolTable.root.getItem(FunctionItem.START_KEY +
                    functionDeclaration.getFunctionName().getName());

//            System.out.println(functionDeclaration.getFunctionName().getName());

            ArrayList<Type> currentArgTypes = functionItem.getArgumentTypes();

            for (int i = 0; i < functionDeclaration.getArgs().size(); i++) {
                VarItem argItem = new VarItem(functionDeclaration.getArgs().get(i).getName());
                argItem.setType(currentArgTypes.get(i));
                try {
                    SymbolTable.top.put(argItem);
                }catch (ItemAlreadyExists ignored){}
            }
        }catch (ItemNotFound ignored){}
//        for(Statement statement : functionDeclaration.getBody())
//            statement.accept(this);
//        System.out.println("hello???");
        //TODO:Figure out whether return types of functions are not the same. !!!
//        Type firstRet = null;

        for(Statement statement : functionDeclaration.getBody()) {
//            if (statement instanceof ReturnStatement){
//                firstRet = ((ReturnStatement) statement).getReturnExp().accept(this);
////                System.out.println(firstRet);
////                System.out.println(statement.getLine());
////                System.out.println("in func!");
////                System.out.println(functionDeclaration.getFunctionName().getName());
////                System.out.println(firstRet);
//            }
//            else {
                statement.accept(this);
//            }
        }

        SymbolTable.pop();
        //TODO:Return the inferred type of the function. !!!
        Type t = retTypes.iterator().next();
        retTypes.clear();
//        System.out.println(firstRet);
        return t;
    }
    @Override
    public Type visit(PatternDeclaration patternDeclaration){
        SymbolTable.push(new SymbolTable());
        try {
            PatternItem patternItem = (PatternItem) SymbolTable.root.getItem(PatternItem.START_KEY +
                    patternDeclaration.getPatternName().getName());
            VarItem varItem = new VarItem(patternDeclaration.getTargetVariable());
            varItem.setType(patternItem.getTargetVarType());
            try {
                SymbolTable.top.put(varItem);
            }catch (ItemAlreadyExists ignored){}
            for(Expression expression : patternDeclaration.getConditions()){
                if(!(expression.accept(this) instanceof BoolType)){
                    typeErrors.add(new ConditionIsNotBool(expression.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                    SymbolTable.pop();
                    return new NoType();
                }
            }
            //TODO:1-figure out whether return expression of different cases in pattern are of the same type/2-return the infered type !!!

            ArrayList<Type> returnTypes = new ArrayList<>();

            for(Expression exp: patternDeclaration.getReturnExp())
                returnTypes.add(exp.accept(this));

            Type firstRet = returnTypes.get(0);

//            for(Type type : returnTypes)
//                if(!firstRet.sameType(type)){
//                    typeErrors.add(new PatternIncompatibleReturnTypes(patternDeclaration.getLine(), patternDeclaration.getPatternName().getName()));
//                    SymbolTable.pop();
//                    return new NoType();
//                }

            SymbolTable.pop();
            return firstRet;

        }catch (ItemNotFound ignored){}
//        SymbolTable.pop();
        return new NoType();
    }

    @Override
    public Type visit(MainDeclaration mainDeclaration){
        //TODO:visit main !!!
        SymbolTable.push(new SymbolTable()); // push main's scope

        for(Statement statement: mainDeclaration.getBody()){
            statement.accept(this);
        }

        SymbolTable.pop();
        return new NoType();
    }

    @Override
    public Type visit(AccessExpression accessExpression){
        if(accessExpression.isFunctionCall()){
            ArrayList<Type> argTypes = new ArrayList<>();
            ArrayList<Expression> arguments = accessExpression.getArguments();
            for (Expression exp : arguments){
                argTypes.add(exp.accept(this));
            }

            //TODO:function is called here.set the arguments type and visit its declaration
            try {
                Identifier functionName = (Identifier) accessExpression.getAccessedExpression();

                FunctionItem functionItem = (FunctionItem) SymbolTable.root.getItem(FunctionItem.START_KEY + functionName.getName());
//                System.out.println(functionItem);
//                System.out.println(accessExpression.getLine());

                int n = functionItem.getFunctionDeclaration().getArgs().size();
                for (int i = 0; i < n; i++){
                    if (functionItem.getFunctionDeclaration().getArgs().get(i).getDefaultVal() != null) {
                        Type type = functionItem.getFunctionDeclaration().getArgs().get(i).getDefaultVal().accept(this);
                        argTypes.add(type);
                    }
                }

                functionItem.setArgumentTypes(argTypes);

                return functionItem.getFunctionDeclaration().accept(this);

            }catch (ItemNotFound ignored){}
        }
        else{
            Type accessedType = accessExpression.getAccessedExpression().accept(this);

            if(!(accessedType instanceof StringType) && !(accessedType instanceof ListType)){
                typeErrors.add(new IsNotIndexable(accessExpression.getLine()));
//                System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                return new NoType();
            }

            //TODO:index of access list must be int !!!
            for (Expression exp : accessExpression.getDimentionalAccess()){

                if (!(exp.accept(this) instanceof IntType)){
                    typeErrors.add(new AccessIndexIsNotInt(accessExpression.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                    return new NoType();
                }
            }
            if (accessedType instanceof ListType)
                return ((ListType)accessedType).getType();
            else{
                return new StringType();
            }

        }
        return null;
    }

    @Override
    public Type visit(ReturnStatement returnStatement){
        Type t = returnStatement.getReturnExp().accept(this);
        retTypes.add(t);
        // TODO:Visit return statement.Note that return type of functions are specified here !!!
        return t;
    }
    @Override
    public Type visit(ExpressionStatement expressionStatement){
        return expressionStatement.getExpression().accept(this);

    }
    @Override
    public Type visit(ForStatement forStatement){
        SymbolTable.push(SymbolTable.top.copy());
//        forStatement.getRangeExpression().accept(this);
        VarItem varItem = new VarItem(forStatement.getIteratorId());
        Type t = forStatement.getRangeExpression().accept(this);
        if (t instanceof StringType){
            varItem.setType(new StringType());
        }
        else if(t instanceof NoType){
            varItem.setType(new NoType());
        }
//        else if(type instanceof ListType){
//            varItem.setType(((ListType) type).getType());
//        }
        else{
            varItem.setType(((ListType) t).getType());
        }
        try{
            SymbolTable.top.put(varItem);
        }catch (ItemAlreadyExists ignored){}

        for(Statement statement : forStatement.getLoopBodyStmts())
            statement.accept(this);
        SymbolTable.pop();
        return null;
    }
    @Override
    public Type visit(IfStatement ifStatement){
        SymbolTable.push(SymbolTable.top.copy());
        for(Expression expression : ifStatement.getConditions())
            if(!(expression.accept(this) instanceof BoolType))
                typeErrors.add(new ConditionIsNotBool(expression.getLine()));
        for(Statement statement : ifStatement.getThenBody())
            statement.accept(this);
        for(Statement statement : ifStatement.getElseBody())
            statement.accept(this);
        SymbolTable.pop();
        return new NoType();
    }
    @Override
    public Type visit(LoopDoStatement loopDoStatement){
        SymbolTable.push(SymbolTable.top.copy());
        for(Statement statement : loopDoStatement.getLoopBodyStmts())
            statement.accept(this);
        SymbolTable.pop();
        return new NoType();
    }
    @Override
    public Type visit(AssignStatement assignStatement){

        if(assignStatement.isAccessList()){
            // TODO:assignment to list ???
            Identifier assignedId = assignStatement.getAssignedId();
            Expression assignExpression = assignStatement.getAssignExpression();
            Expression accessListExpression = assignStatement.getAccessListExpression();
            Type assignedIdType = null;

            try {
                assignedIdType = ((VarItem) SymbolTable.top.getItem(VarItem.START_KEY + assignedId.getName())).getType();
            }catch (ItemNotFound ignored){}

            Type accessListType = accessListExpression.accept(this);
            if(!(accessListType instanceof IntType)){
                typeErrors.add(new AccessIndexIsNotInt(accessListExpression.getLine()));
                return new NoType();
            }

            Type assignType = assignExpression.accept(this);
            if(!((ListType) assignedIdType).getType().sameType(assignType)){
                typeErrors.add(new ListElementsTypesMisMatch(assignStatement.getLine()));
                return new NoType();
            }
            return new NoType();
        }

        else{
            VarItem newVarItem = new VarItem(assignStatement.getAssignedId());

            Expression exp = assignStatement.getAssignExpression();
            AssignOperator assignOp = assignStatement.getAssignOperator();

            Type rightType = exp.accept(this);

            if (assignOp == AssignOperator.ASSIGN){
//                System.out.println(rightType);
//                System.out.println(assignStatement.getLine());
                newVarItem.setType(rightType);
            }
            else{
                Type varType= null;
                try {
                    varType = ((VarItem) SymbolTable.top.getItem(VarItem.START_KEY + assignStatement.getAssignedId().getName())).getType();
                }catch (ItemNotFound ignored){}
                if(varType instanceof IntType && rightType instanceof IntType){
                    newVarItem.setType(new IntType());
                }
                else if(varType instanceof FloatType && rightType instanceof FloatType){
                    newVarItem.setType(new FloatType());
                }
                else{
                    typeErrors.add(new UnsupportedOperandType(assignStatement.getLine(), assignOp.toString()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                    return new NoType();
                }
            }
            // TODO:maybe new type for a variable !!!
            try {
                SymbolTable.top.put(newVarItem);
            }catch (ItemAlreadyExists ignored){}
        }
        return new NoType();
    }
    @Override
    public Type visit(BreakStatement breakStatement){
        for(Expression expression : breakStatement.getConditions())
            if(!((expression.accept(this)) instanceof BoolType))
                typeErrors.add(new ConditionIsNotBool(expression.getLine()));

        return null;
    }
    @Override
    public Type visit(NextStatement nextStatement){
        for(Expression expression : nextStatement.getConditions())
            if(!((expression.accept(this)) instanceof BoolType))
                typeErrors.add(new ConditionIsNotBool(expression.getLine()));

        return null;
    }
    @Override
    public Type visit(PushStatement pushStatement){
        //TODO:visit push statement !!!

        Expression initialVar = pushStatement.getInitial();
        Expression toBeAdded = pushStatement.getToBeAdded();
        Type initialType = initialVar.accept(this);
        Type toBeAddedType = toBeAdded.accept(this);

        if(!(initialType instanceof ListType) && !(initialType instanceof StringType)){
            typeErrors.add(new IsNotPushedable(initialVar.getLine()));
//            System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
            return new NoType();
        }

        if(initialType instanceof ListType){
            if(!initialType.sameType(toBeAddedType)){ /// ???
                typeErrors.add(new PushArgumentsTypesMisMatch(pushStatement.getLine()));
//                System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                return new NoType();
            }
        }

        if(initialType instanceof StringType && !(toBeAddedType instanceof StringType)){
            typeErrors.add(new PushArgumentsTypesMisMatch(pushStatement.getLine()));
//            System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
            return new NoType();
        }

        return new NoType();
    }
    @Override
    public Type visit(PutStatement putStatement){
        //TODO:visit putStatement !!!
        putStatement.getExpression().accept(this);
        return new NoType();

    }
    @Override
    public Type visit(BoolValue boolValue){
        return new BoolType();
    }
    @Override
    public Type visit(IntValue intValue){
        return new IntType();
    }
    @Override
    public Type visit(FloatValue floatValue){return new FloatType();}
    @Override
    public Type visit(StringValue stringValue){
        return new StringType();
    }
    @Override
    public Type visit(ListValue listValue){
        // TODO:visit listValue !!!
        ArrayList<Expression> elements = listValue.getElements();
        ArrayList<Type> elementTypes = new ArrayList<>();
        for(Expression expression : elements)
            elementTypes.add(expression.accept(this));

        Type firstElement = new NoType();

        if(elementTypes.get(0) instanceof BoolType){
            firstElement = new BoolType();
            for(Type type : elementTypes)
                if(!(type instanceof BoolType)) {
                    typeErrors.add(new ListElementsTypesMisMatch(listValue.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                }
        }

        if(elementTypes.get(0) instanceof StringType){
            firstElement = new StringType();
            for(Type type : elementTypes)
                if(!(type instanceof StringType)) {
                    typeErrors.add(new ListElementsTypesMisMatch(listValue.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                }
        }

        if(elementTypes.get(0) instanceof IntType){
            firstElement = new IntType();
            for(Type type : elementTypes)
                if(!(type instanceof IntType)) {
                    typeErrors.add(new ListElementsTypesMisMatch(listValue.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                }
        }

        if(elementTypes.get(0) instanceof FloatType){
            firstElement = new FloatType();
            for(Type type : elementTypes)
                if(!(type instanceof FloatType)) {
                    typeErrors.add(new ListElementsTypesMisMatch(listValue.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                }
        }

        if (!(firstElement instanceof NoType))
            return new ListType(firstElement);
        else
            return firstElement;
    }
    @Override
    public Type visit(FunctionPointer functionPointer){
        return new FptrType(functionPointer.getId().getName());
    }
    @Override
    public Type visit(AppendExpression appendExpression){

        Type appendeeType = appendExpression.getAppendee().accept(this);
//        System.out.println(appendeeType);
        ArrayList<Expression> appendedExps = appendExpression.getAppendeds();

        if(!(appendeeType instanceof ListType) && !(appendeeType instanceof StringType)){
            typeErrors.add(new IsNotAppendable(appendExpression.getLine()));
//            System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
            return new NoType();
        }

        if (appendeeType instanceof StringType){
            for(Expression exp:appendedExps){
                Type appendedType = exp.accept(this);
//                System.out.println(appendedType);
                if (!appendedType.sameType(appendeeType)){
                    typeErrors.add(new AppendTypesMisMatch(appendExpression.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                }
            }
        }

        if (appendeeType instanceof ListType){
            Type elementType = ((ListType)appendeeType).getType();
            for(Expression exp:appendedExps){
                Type appendedType = exp.accept(this);
//                System.out.println(appendedType);
                if (!appendedType.sameType(elementType)){
                    typeErrors.add(new AppendTypesMisMatch(appendExpression.getLine()));
//                    System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                }
            }
        }

        return appendeeType;
    }
    @Override
    public Type visit(BinaryExpression binaryExpression){
        //TODO:visit binary expression !!!
        BinaryOperator op = binaryExpression.getOperator();
        Expression first = binaryExpression.getFirstOperand();
        Expression second = binaryExpression.getSecondOperand();

        Type firstType = first.accept(this);
        Type secondType = second.accept(this);

//        System.out.println(firstType);
//        System.out.println(secondType);

//        System.out.println(firstType);
//        System.out.println(secondType);
//        System.out.println(binaryExpression.getLine());

//        if (!(firstType.sameType(secondType))){
//            typeErrors.add(new NonSameOperands(binaryExpression.getLine(), op));
//            System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
//            return new NoType();
//        }

//        System.out.println(firstType);
//        System.out.println(secondType);


        if(op == BinaryOperator.DIVIDE || op == BinaryOperator.MINUS || op == BinaryOperator.MULT || op == BinaryOperator.PLUS){

            if(firstType instanceof IntType && secondType instanceof IntType)
                return new IntType();
            else if(firstType instanceof FloatType && secondType instanceof FloatType)
                return new FloatType();
            else if(firstType instanceof IntType && secondType instanceof NoType)
                return new NoType();
            else if(firstType instanceof NoType && secondType instanceof IntType)
                return new NoType();
            else if(firstType instanceof FloatType && secondType instanceof NoType)
                return new NoType();
            else if(firstType instanceof NoType && secondType instanceof FloatType)
                return new NoType();

            else{
                typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), op.toString()));
                return new NoType();
            }
        }

        if (op == BinaryOperator.LESS_THAN || op == BinaryOperator.LESS_EQUAL_THAN || op == BinaryOperator.GREATER_THAN || op == BinaryOperator.GREATER_EQUAL_THAN){
            if(firstType instanceof IntType && secondType instanceof IntType)
                return new BoolType();
            else if(firstType instanceof FloatType && secondType instanceof NoType)
                return new BoolType();
            else if(firstType instanceof BoolType && secondType instanceof BoolType)
                return new BoolType();
            else if(firstType instanceof StringType && secondType instanceof StringType)
                return new BoolType();
            else if(firstType instanceof ListType && secondType instanceof ListType)
                return new BoolType();
            else if(firstType instanceof FloatType && secondType instanceof FloatType)
                return new BoolType();
            else if(firstType instanceof IntType && secondType instanceof NoType)
                return new BoolType();
            else if(firstType instanceof NoType && secondType instanceof IntType)
                return new BoolType();
            else if(firstType instanceof NoType && secondType instanceof FloatType)
                return new BoolType();

            else{
                typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), op.toString()));
                return new NoType();
            }
        }

        if (op == BinaryOperator.EQUAL || op == BinaryOperator.NOT_EQUAL){
            if(firstType instanceof IntType && secondType instanceof IntType)
                return new BoolType();
            else if(firstType instanceof FloatType && secondType instanceof FloatType)
                return new BoolType();
            else if(firstType instanceof IntType && secondType instanceof NoType)
                return new BoolType();
            else if(firstType instanceof NoType && secondType instanceof IntType)
                return new BoolType();
            else if(firstType instanceof FloatType && secondType instanceof NoType)
                return new BoolType();
            else if(firstType instanceof NoType && secondType instanceof FloatType)
                return new BoolType();
            else if(firstType instanceof BoolType && secondType instanceof BoolType)
                return new BoolType();
            else if(firstType instanceof StringType && secondType instanceof StringType)
                return new BoolType();
            else if(firstType instanceof ListType && secondType instanceof ListType)
                return new BoolType();

            else{
                typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), op.toString()));
                return new NoType();
            }
        }

        return null;
    }
    @Override
    public Type visit(UnaryExpression unaryExpression){
        //TODO:visit unaryExpression !!!

        UnaryOperator op = unaryExpression.getOperator();
        Expression operand = unaryExpression.getExpression();
        Type opType = operand.accept(this);

        if(op == UnaryOperator.NOT){
            if(opType instanceof BoolType)
                return new BoolType();

            typeErrors.add(new UnsupportedOperandType(unaryExpression.getLine(), op.toString()));
            return new NoType();
        }

        if(op == UnaryOperator.INC || op == UnaryOperator.DEC || op == UnaryOperator.MINUS){
            if (opType instanceof IntType)
                return new IntType();
            else if (opType instanceof FloatType)
                return new FloatType();

            else{
                typeErrors.add(new UnsupportedOperandType(unaryExpression.getLine(), op.toString()));
                return new NoType();
            }
        }
        return null;
    }
    @Override
    public Type visit(ChompStatement chompStatement){
        if (!(chompStatement.getChompExpression().accept(this) instanceof StringType)) {
            typeErrors.add(new ChompArgumentTypeMisMatch(chompStatement.getLine()));
//            System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
            return new NoType();
        }

        return new StringType();
    }
    @Override
    public Type visit(ChopStatement chopStatement){
        Expression chopExp = chopStatement.getChopExpression();
        if (!(chopExp.accept(this) instanceof StringType)){
            typeErrors.add(new ChopArgumentTypeMisMatch(chopExp.getLine()));
//            System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
            return new NoType();
        }
        return new StringType();
    }
    @Override
    public Type visit(Identifier identifier){
        // TODO:visit Identifier !!!

        try {
            VarItem varItem = (VarItem) SymbolTable.top.getItem(VarItem.START_KEY + identifier.getName());
            return varItem.getType();
        } catch (ItemNotFound ignored){}

        return null;
    }
    @Override
    public Type visit(LenStatement lenStatement){
        //TODO:visit LenStatement.Be carefull about the return type of LenStatement. !!!
        Expression exp = lenStatement.getExpression();
        Type expType = exp.accept(this);

        if (!(expType instanceof StringType || expType instanceof ListType)){
            typeErrors.add(new LenArgumentTypeMisMatch(lenStatement.getLine()));
//            System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
            return new NoType();
        }

        return new IntType();
    }
    @Override
    public Type visit(MatchPatternStatement matchPatternStatement){
        try{
            PatternItem patternItem = (PatternItem)SymbolTable.root.getItem(PatternItem.START_KEY +
                    matchPatternStatement.getPatternId().getName());
            patternItem.setTargetVarType(matchPatternStatement.getMatchArgument().accept(this));
            return patternItem.getPatternDeclaration().accept(this);
        }catch (ItemNotFound ignored){}
        return new NoType();
    }
    @Override
    public Type visit(RangeExpression rangeExpression){

        ArrayList<Expression> rangeExpressions = rangeExpression.getRangeExpressions();
        RangeType rangeType = rangeExpression.getRangeType();

        if(rangeType.equals(RangeType.LIST)){
            // TODO --> mind that the lists are declared explicitly in the grammar in this node, so handle the errors ???

            ListValue list = (ListValue) rangeExpressions.get(0);
            Type type = list.accept(this);

            if(!(type instanceof ListType)){
                typeErrors.add(new RangeValuesMisMatch(rangeExpression.getLine()));
//                System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                return new NoType();
            }
            else{
                Type first = rangeExpressions.get(0).accept(this);
                for (Expression exp:rangeExpressions){
                    if (!(first.sameType(exp.accept(this)))){
                        typeErrors.add(new ListElementsTypesMisMatch(rangeExpression.getLine()));
//                        System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                        return new NoType();
                    }
                }
                return new ListType(first);
            }
        }

        if(rangeType.equals(RangeType.IDENTIFIER)){
            Type identifierType = rangeExpressions.get(0).accept(this);
            if(!(identifierType instanceof ListType)){
                typeErrors.add(new IsNotIterable(rangeExpression.getLine()));
//                System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                return new NoType();
            }
            return identifierType;
        }

        if(rangeType.equals(RangeType.DOUBLE_DOT)){
            Type start = rangeExpressions.get(0).accept(this);
            Type end = rangeExpressions.get(1).accept(this);
//            System.out.println("dotdot");
//            System.out.println(start);
//            System.out.println(end);
//            System.out.println(rangeExpression.getLine());

            if (start instanceof IntType && end instanceof IntType){
                return new ListType(new IntType());
            }
            else{
                typeErrors.add(new RangeValuesMisMatch(rangeExpression.getLine()));
//                System.out.println(typeErrors.get(typeErrors.size() - 1).getErrorMessage());
                return new NoType();
            }
        }

        return new NoType();
    }
}
