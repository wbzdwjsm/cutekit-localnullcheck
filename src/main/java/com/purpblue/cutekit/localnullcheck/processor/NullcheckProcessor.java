package com.purpblue.cutekit.localnullcheck.processor;

import com.purpblue.cutekit.localnullcheck.annotation.Exclude;
import com.purpblue.cutekit.localnullcheck.annotation.Indicator;
import com.purpblue.cutekit.localnullcheck.annotation.NullCheck;
import com.purpblue.cutekit.localnullcheck.annotation.ProcType;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Purpblue
 */
@SupportedAnnotationTypes("com.purpblue.cutekit.localnullcheck.annotation.NullCheck")
public class NullcheckProcessor extends AbstractProcessor {

    private TreeMaker treeMaker;
    private JavacTrees javacTrees;
    private Names names;

    private static final String STR_EX_INFO = "exInfo";
    private static final String STR_PROC_TYPE = "procType";
    private static final String STR_NEW_CLASS = "newClass";
    private static final String STR_NEW_UPPER = "NEW";
    private static final String NPE = "NullPointerException";
    private static final String STRING = "String";
    private static final String[] WRAPPER_NAMES = {
            "Integer", "Short", "Byte", "Long",
            "Float", "Double", "Boolean", "Character",
            "java.lang.Integer", "java.lang.Short", "java.lang.Byte", "java.lang.Long",
            "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character"
    };

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        treeMaker = TreeMaker.instance(context);
        javacTrees = JavacTrees.instance(processingEnv);
        names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> checkSet = roundEnv.getElementsAnnotatedWith(NullCheck.class);
        for (Element e : checkSet) {
            JCTree jcTree = javacTrees.getTree(e);
            jcTree.accept(new TreeTranslator() {
                @Override
                public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                    super.visitMethodDef(jcMethodDecl);
                    outter:
                    for (JCTree.JCStatement b : jcMethodDecl.body.stats) {
                        boolean canProcess = false;
                        JCTree.JCVariableDecl vd = null;
                        if (b.getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
                            //Reassign
                            JCTree.JCExpressionStatement vb = (JCTree.JCExpressionStatement) b;
                            if (vb.expr.getKind() == Tree.Kind.ASSIGNMENT) {
                                JCTree.JCAssign jag = (JCTree.JCAssign) vb.expr;
                                if (jag.rhs.getKind() != Tree.Kind.METHOD_INVOCATION) {
                                    continue;
                                }
                                JCTree.JCIdent jci = (JCTree.JCIdent) jag.lhs;
                                for (JCTree.JCStatement s : jcMethodDecl.body.stats) {
                                    if (s.getKind() == Tree.Kind.VARIABLE) {
                                        JCTree.JCVariableDecl vs = (JCTree.JCVariableDecl) s;
                                        if (vs.name.equals(jci.name)) {
                                            //Excluding primitives and Wrappers
                                            if (vs.vartype.getKind() == Tree.Kind.PRIMITIVE_TYPE
                                                    || isWrapperType(vs.vartype.toString())) {
                                                continue outter;
                                            }
                                            vd = vs;
                                            break;
                                        }
                                    }
                                }
                                canProcess = true;
                            }
                        } else if (b.getKind() == Tree.Kind.VARIABLE) {
                            //Create a local variable
                            JCTree.JCVariableDecl vb = (JCTree.JCVariableDecl) b;
                            //Excluding primitives and Wrappers
                            if (vb.vartype.getKind() == Tree.Kind.PRIMITIVE_TYPE
                                    || vb.init.getKind() != Tree.Kind.METHOD_INVOCATION
                                    || isWrapperType(vb.vartype.toString())) {
                                continue;
                            }

                            JCTree.JCAnnotation excludeAnno = findAnnotation(vb, Exclude.class);
                            if (excludeAnno != null) {
                                continue;
                            }
                            vd = vb;
                            canProcess = true;
                        }

                        if (canProcess) {
                            JCTree.JCAnnotation jcInclude = findAnnotation(vd, Indicator.class);
                            ProcType procType;
                            String exInfo = null;
                            String newClass = null;
                            if (jcInclude != null) {
                                Map<String, Serializable> includeInfo = indicateInfo(jcInclude);
                                procType = (ProcType) includeInfo.get(STR_PROC_TYPE);
                                if (procType == null) {
                                    procType = ProcType.THROW_EXCEPTION;
                                }
                                if (procType == ProcType.THROW_EXCEPTION) {
                                    exInfo = (String) includeInfo.get(STR_EX_INFO);
                                } else {
                                    newClass = (String) includeInfo.get(STR_NEW_CLASS);
                                    newClass = newClass.substring(0, newClass.lastIndexOf("."));
                                }
                            } else {
                                procType = ProcType.THROW_EXCEPTION;
                            }
                            if (exInfo == null || "".equals(exInfo)) {
                                exInfo = vd.vartype.toString() + ":" + vd.name.toString() + " cannot be null!";
                            }
                            //Must not be primitives or wrappers
                            JCTree.JCExpression condition = treeMaker.Binary(JCTree.Tag.EQ, treeMaker.Ident(vd.name), treeMaker.Literal(TypeTag.BOT, "null"));
                            JCTree.JCIf anIf;
                            if (procType == ProcType.THROW_EXCEPTION) {
                                JCTree.JCExpression npe = treeMaker.NewClass(
                                        null,
                                        List.of(treeMaker.Ident(names.fromString(STRING))),
                                        treeMaker.Ident(names.fromString(NPE)),
                                        List.of(treeMaker.Literal(exInfo)),
                                        null
                                );
                                JCTree.JCStatement ifStat = treeMaker.Throw(npe);
                                anIf = treeMaker.If(
                                        condition,
                                        ifStat,
                                        null
                                );
                            } else {
                                JCTree.JCExpression newObject = treeMaker.NewClass(
                                        null,
                                        List.nil(),
                                        classPath(newClass),
                                        List.nil(),
                                        null
                                );
                                JCTree.JCExpressionStatement ifStat = treeMaker.Exec(treeMaker.Assign(treeMaker.Ident(vd.name), newObject));
                                anIf = treeMaker.If(
                                        condition,
                                        ifStat,
                                        null
                                );
                            }
                            //add new statements to the method body
                            jcMethodDecl.body.stats = addStatements(b, anIf, jcMethodDecl.body.stats);
                        }
                    }
                }
            });
        }
        return true;
    }

    private boolean isWrapperType(String s) {
        return Arrays.binarySearch(WRAPPER_NAMES, s) > -1;
    }

    /**
     * Insert a new stat into the body.stat of the method
     *
     * @param followedStat
     * @param statement
     * @param stats        jcMethodDecl.body.stats
     * @return
     */
    private List<JCTree.JCStatement> addStatements(JCTree.JCStatement followedStat, JCTree.JCStatement statement, List<JCTree.JCStatement> stats) {
        List<JCTree.JCStatement> newStats = List.nil();
        for (JCTree.JCStatement c : stats) {
            newStats = newStats.append(c);
            if (c == followedStat) {
                newStats = newStats.append(statement);
            }
        }
        return newStats;
    }


    /**
     * Find the annotation due to the given klass.
     *
     * @param vd
     * @param klass
     * @param <T>
     * @return
     */
    private <T> JCTree.JCAnnotation findAnnotation(JCTree.JCVariableDecl vd, Class<T> klass) {
        List<JCTree.JCAnnotation> annotations = vd.getModifiers().getAnnotations();
        if (annotations.isEmpty()) {
            return null;
        }
        String annoName = klass.getSimpleName();
        for (JCTree.JCAnnotation annotation : annotations) {
            if (annotation.annotationType.toString().equals(annoName)
                    || annotation.annotationType.toString().equals(klass.getCanonicalName())) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Find Indicator annotation, is the implementation the Best Practice?
     *
     * @param indicateAnno
     * @return
     */
    private Map<String, Serializable> indicateInfo(JCTree.JCAnnotation indicateAnno) {
        Map<String, Serializable> reMap = new HashMap<>(4);
        for (JCTree.JCExpression expression : indicateAnno.args) {
            JCTree.JCAssign assign = (JCTree.JCAssign) expression;
            switch (assign.lhs.toString()) {
                case STR_EX_INFO:
                case STR_NEW_CLASS:
                    reMap.put(assign.lhs.toString(), assign.rhs.toString().replace("\"", ""));
                    break;
                case STR_PROC_TYPE:
                    JCTree.JCFieldAccess jfa = (JCTree.JCFieldAccess) assign.rhs;
                    if (STR_NEW_UPPER.equals(jfa.name.toString())) {
                        reMap.put(assign.lhs.toString(), ProcType.NEW);
                    }
                    break;
                default:
                    break;
            }
        }
        return reMap;
    }

    /**
     * Getting appropriate JCExpression from a fully qualified class name
     *
     * @param path
     * @return
     */
    private JCTree.JCExpression classPath(String path) {
        Assert.checkNonNull(path, "path mustn't be null!");
        String[] cArray = path.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(names.fromString(cArray[0]));
        for (int i = 1; i < cArray.length; i++) {
            expr = treeMaker.Select(expr, names.fromString(cArray[i]));
        }
        return expr;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
