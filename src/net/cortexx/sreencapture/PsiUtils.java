package net.cortexx.sreencapture;

import com.intellij.psi.*;

public class PsiUtils {
    public static class MethodParameter {
        PsiMethodCallExpression methodCall;
        PsiMethod methodDecl;
        PsiParameter paramDecl;
        PsiExpression paramExpr;
        PsiElement underCaret;
    }

    public static class FieldOrVariable {
        PsiExpression underCaret;
        PsiVariable left;
        PsiExpression right;
    }

    public static MethodParameter fromCaretOnMethodParameter(PsiElement underCaret) {
        PsiElement current = underCaret;
        PsiElement last = null;
        for (int i=0; i<10 && current != null; ++i) {
            last = current;
            current = current.getParent();
            if (current instanceof PsiExpressionList) break;
        }
        if (!(current instanceof PsiExpressionList)) return null;
        if (!(current.getParent() instanceof PsiMethodCallExpression)) return null;
        if (!(last instanceof PsiExpression)) return null;

        MethodParameter r = new MethodParameter();
        r.methodCall = (PsiMethodCallExpression) current.getParent();
        r.underCaret = exprUnderCaret(underCaret, r.methodCall);
        r.paramExpr = (PsiExpression) last;

        PsiExpression[] paramExprs = ((PsiExpressionList)current).getExpressions();

        int paramIndex = 0;
        for (;paramIndex<paramExprs.length; ++paramIndex) {
            if (paramExprs[paramIndex] == last) break;
        }
        if (paramIndex == paramExprs.length) return null;

        r.methodDecl = r.methodCall.resolveMethod();
        if (r.methodDecl == null) return null;

        PsiParameter[] params = r.methodDecl.getParameterList().getParameters();
        if (paramIndex < params.length) {
            r.paramDecl = params[paramIndex];
        } else if (params.length > 0 && params[params.length-1].isVarArgs()) {
            r.paramDecl = params[params.length-1];
        }

        return r;
    }

    public static FieldOrVariable fromCaretOnAssigment(PsiElement underCaret) {
        PsiElement current = underCaret;
        PsiElement last = null;
        for (int i=0; i<10 && current != null; ++i) {
            last = current;
            current = current.getParent();
            if (current instanceof PsiAssignmentExpression
                    || current instanceof PsiLocalVariable
                    || current instanceof PsiField)
                break;
        }
        if (current instanceof PsiAssignmentExpression) {
            PsiAssignmentExpression assignment = (PsiAssignmentExpression) current;
            if (!(assignment.getLExpression() instanceof PsiReferenceExpression)) return null;

            FieldOrVariable r = new FieldOrVariable();
            r.underCaret = exprUnderCaret(underCaret, current);
            r.right = assignment.getRExpression();
            r.left = (PsiVariable) ((PsiReferenceExpression) assignment.getLExpression()).resolve();

            return r;
        }
        else if (current instanceof PsiField || current instanceof PsiLocalVariable) {
            FieldOrVariable r = new FieldOrVariable();
            if (last instanceof PsiExpression) {
                r.right = (PsiExpression) last;
                r.underCaret = exprUnderCaret(underCaret, current);
            }
            r.left = (PsiVariable) current;

            return r;
        }
        return null;
    }

    public static PsiExpression exprUnderCaret(PsiElement underCaret, PsiElement stopAt) {
        PsiElement e=underCaret;
        for (int i=0; i<10 && e != null && e != stopAt; ++i) {
            if (e instanceof PsiLiteralExpression) return (PsiExpression) e;
            e = e.getParent();
        }
        return null;
    }
}
