import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class SimpleLangInterpreter extends AbstractParseTreeVisitor<Integer> implements SimpleLangVisitor<Integer> {

    private final Map<String, SimpleLangParser.DecContext> global_funcs = new HashMap<>();
    private final Stack<Map<String, Integer>> frames = new Stack<>();

    public Integer visitProgram(SimpleLangParser.ProgContext ctx, String[] args)
    {

        for (SimpleLangParser.DecContext dec : ctx.dec()) {
            SimpleLangParser.Typed_idfrContext typedIdfr = dec.typed_idfr(0);
            global_funcs.put(typedIdfr.Idfr().getText(), dec);
        }

        SimpleLangParser.DecContext main = global_funcs.get("main");

        Map<String, Integer> newFrame = new HashMap<>();
        int argIndex = 0;

        for (int i = 0; i < args.length; ++i) {
            String argValue = args[i];
            int value = argValue.equals("true") ? 1 : (argValue.equals("false") ? 0 : Integer.parseInt(argValue));
            newFrame.put(main.vardec.get(i).stop.getText(), value);
        }
        frames.push(newFrame);
        return visit(main);

    }

    @Override public Integer visitDec(SimpleLangParser.DecContext ctx)
    {
        Integer returnValue = visit(ctx.body());
        frames.pop();
        return returnValue;
    }

    @Override public Integer visitBody(SimpleLangParser.BodyContext ctx) {

        for (SimpleLangParser.DeclarationsContext dtxc : ctx.declarations()) {
            visit(dtxc);
        }

        Integer returnValue = null;
        for (SimpleLangParser.ExpContext exp : ctx.ene) {
            returnValue = visit(exp);
        }
        return returnValue;
    }

    @Override public Integer visitBlock(SimpleLangParser.BlockContext ctx)
    {
        Integer returnValue = null;
        for (int i = 0; i < ctx.ene.size(); ++i) {
            SimpleLangParser.ExpContext exp = ctx.ene.get(i);
            returnValue = visit(exp);
        }
        return returnValue;
    }

    @Override
    public Integer visitDeclarations(SimpleLangParser.DeclarationsContext ctx) {

        SimpleLangParser.ExpContext value = ctx.exp();
        String varName = ctx.typed_idfr().Idfr().getText();
        frames.peek().put(varName, visit(value));
        return null;
    }

    @Override public Integer visitAssignExpr(SimpleLangParser.AssignExprContext ctx)
    {
        SimpleLangParser.ExpContext value = ctx.exp();
        String varName = ctx.Idfr().getText();
        frames.peek().replace(varName, visit(value));
        return null;
    }


    @Override public Integer visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx) {

        SimpleLangParser.ExpContext operand1 = ctx.exp(0);
        SimpleLangParser.ExpContext operand2 = ctx.exp(1);

        int oprnd1 = visit(operand1);
        int oprnd2 = visit(operand2);

        return switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) {
            case SimpleLangParser.Eq -> (Objects.equals(oprnd1, oprnd2)) ? 1 : 0;
            case SimpleLangParser.Less -> (oprnd1 < oprnd2) ? 1 : 0;
            case SimpleLangParser.LessEq -> (oprnd1 <= oprnd2) ? 1 : 0;
            case SimpleLangParser.Plus -> oprnd1 + oprnd2;
            case SimpleLangParser.Minus -> oprnd1 - oprnd2;
            case SimpleLangParser.Times -> oprnd1 * oprnd2;
            case SimpleLangParser.Greater -> (oprnd1 > oprnd2) ? 1 : 0;
            case SimpleLangParser.GreaterEq -> (oprnd1 >= oprnd2) ? 1 : 0;
            case SimpleLangParser.Divide -> {
                if (oprnd2 != 0) { yield (int) ((double) oprnd1 / oprnd2);}
                else {throw new RuntimeException("Cannot divide by zero.");}
            }
            case SimpleLangParser.And -> oprnd1 & oprnd2;
            case SimpleLangParser.Or -> oprnd1 | oprnd2;
            case SimpleLangParser.Xor -> oprnd1 ^ oprnd2;
            default -> throw new RuntimeException("Shouldn't be here - wrong binary operator.");
        };


    }
    @Override public Integer visitInvokeExpr(SimpleLangParser.InvokeExprContext ctx)
    {
        SimpleLangParser.DecContext declaration = global_funcs.get(ctx.Idfr().getText());
        Map<String, Integer> newFrame = new HashMap<>();

        for (SimpleLangParser.Typed_idfrContext param : declaration.vardec) {
            int argIndex = declaration.vardec.indexOf(param);
            SimpleLangParser.ExpContext argument = ctx.args.get(argIndex);
            newFrame.put(param.Idfr().getText(), visit(argument));
        }

        frames.push(newFrame);
        return visit(declaration);


    }

    @Override public Integer visitBlockExpr(SimpleLangParser.BlockExprContext ctx) {
        return visit(ctx.block());
    }

    @Override public Integer visitIfExpr(SimpleLangParser.IfExprContext ctx)
    {

        SimpleLangParser.ExpContext cond = ctx.exp();
        Integer condValue = visit(cond);
        if (condValue != 0) {

            SimpleLangParser.BlockContext thenBlock = ctx.block(0);
            return visit(thenBlock);

        } else {

            SimpleLangParser.BlockContext elseBlock = ctx.block(1);
            return visit(elseBlock);
        }

    }

    @Override public Integer visitPrintExpr(SimpleLangParser.PrintExprContext ctx) {

        SimpleLangParser.ExpContext exp = ctx.exp();

        if (((TerminalNode) exp.getChild(0)).getSymbol().getType() == SimpleLangParser.Space) {

            System.out.print(" ");

        } else if (((TerminalNode) exp.getChild(0)).getSymbol().getType() == SimpleLangParser.NewLine) {

            System.out.println();

        } else {

            System.out.print(visit(exp));

        }
        return null;
    }

    @Override
    public Integer visitWhileExpr(SimpleLangParser.WhileExprContext ctx) {

        SimpleLangParser.ExpContext cond = ctx.exp();
        SimpleLangParser.BlockContext whileBlock = ctx.block();

        while (visit(cond) != 0) {
            visit(whileBlock);
        }
        return null;
    }

    @Override
    public Integer visitRepeatExpr(SimpleLangParser.RepeatExprContext ctx) {

        SimpleLangParser.ExpContext cond = ctx.exp();
        SimpleLangParser.BlockContext repeatBlock = ctx.block();

        do {
            visit(repeatBlock);
        } while (visit(cond) == 0);
        return null;
    }

    @Override public Integer visitIdExpr(SimpleLangParser.IdExprContext ctx)
    {
        return frames.peek().get(ctx.Idfr().getText());
    }

    @Override public Integer visitIntExpr(SimpleLangParser.IntExprContext ctx) {
        return Integer.parseInt(ctx.IntLit().getText());
    }

    @Override
    public Integer visitBoolExpr(SimpleLangParser.BoolExprContext ctx) {
        String boolLiteral = ctx.BoolLit().getText();
        return Boolean.parseBoolean(boolLiteral)? 1: 0;
    }

    @Override public Integer visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx) {
        System.out.println(" ");
        return null;
    }
    @Override
    public Integer visitNewLineExpr(SimpleLangParser.NewLineExprContext ctx) {
        System.out.println();
        return null;
    }

    @Override
    public Integer visitSkipExpr(SimpleLangParser.SkipExprContext ctx) {
        return 0;
    }

    @Override public Integer visitProg(SimpleLangParser.ProgContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitTyped_idfr(SimpleLangParser.Typed_idfrContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitType(SimpleLangParser.TypeContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitEqBinop(SimpleLangParser.EqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitLessBinop(SimpleLangParser.LessBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitLessEqBinop(SimpleLangParser.LessEqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitGreaterBinop(SimpleLangParser.GreaterBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitGreaterEqBinop(SimpleLangParser.GreaterEqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitPlusBinop(SimpleLangParser.PlusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitMinusBinop(SimpleLangParser.MinusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitTimesBinop(SimpleLangParser.TimesBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitDivideBinop(SimpleLangParser.DivideBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitAndBinop(SimpleLangParser.AndBinopContext ctx) {
        throw new RuntimeException("Should not be here!");    }

    @Override
    public Integer visitOrBinop(SimpleLangParser.OrBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitXorBinop(SimpleLangParser.XorBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }


}
