/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import elemental.dom.Element;
import elemental.events.KeyboardEvent;
import elemental.events.MouseEvent;
import elemental.html.TableCellElement;
import elemental.html.TableElement;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.debug.Breakpoint;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeResources;
import org.eclipse.che.ide.ext.java.jdi.shared.Location;
import org.eclipse.che.ide.ext.java.jdi.shared.Variable;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.dom.Elements;
import org.eclipse.che.ide.util.input.SignalEvent;
import org.vectomatic.dom.svg.ui.SVGImage;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The implementation of {@link DebuggerView}.
 *
 * @author Andrey Plotnikov
 */
@Singleton
public class DebuggerViewImpl extends BaseView<DebuggerView.ActionDelegate> implements DebuggerView {

    interface DebuggerViewImplUiBinder extends UiBinder<Widget, DebuggerViewImpl> {
    }

    @UiField
    PushButton                      btnResume;
    @UiField
    ToggleButton                    btnStepInto;
    @UiField
    ToggleButton                    btnStepOver;
    @UiField
    ToggleButton                    btnStepReturn;
    @UiField
    PushButton                      btnDisconnect;
    @UiField
    PushButton                      btnRemoveAllBreakpoints;
    @UiField
    PushButton                      btnChangeValue;
    @UiField
    PushButton                      btnEvaluateExpression;
    @UiField
    Label                           vmName;
    @UiField
    Label                           executionPoint;
    @UiField
    ScrollPanel                     variablesPanel;
    @UiField
    ScrollPanel                     breakpointsPanel;
    @UiField(provided = true)
    JavaRuntimeLocalizationConstant locale;
    @UiField(provided = true)
    Resources                       coreRes;
    @UiField(provided = true)
    SplitLayoutPanel splitPanel = new SplitLayoutPanel(3);

    private final DtoFactory                dtoFactory;
    private       SimpleList<Breakpoint>    breakpoints;
    private       Tree<Variable>            variables;
    private       TreeNodeElement<Variable> selectedVariable;
    private       JavaRuntimeResources      res;

    /**
     * Create view.
     *
     * @param partStackUIResources
     * @param resources
     * @param locale
     * @param coreRes
     * @param rendererResources
     */
    @Inject
    protected DebuggerViewImpl(PartStackUIResources partStackUIResources,
                               JavaRuntimeResources resources,
                               JavaRuntimeLocalizationConstant locale,
                               Resources coreRes,
                               VariableTreeNodeRenderer.Resources rendererResources,
                               DtoFactory dtoFactory,
                               DebuggerViewImplUiBinder uiBinder) {
        super(partStackUIResources);

        this.locale = locale;
        this.res = resources;
        this.coreRes = coreRes;
        this.dtoFactory = dtoFactory;

        setContentWidget(uiBinder.createAndBindUi(this));

        btnResume.getElement().appendChild(new SVGImage(resources.resumeButton()).getElement());
        btnStepInto.getElement().appendChild(new SVGImage(resources.stepIntoButton()).getElement());
        btnStepOver.getElement().appendChild(new SVGImage(resources.stepOverButton()).getElement());
        btnStepReturn.getElement().appendChild(new SVGImage(resources.stepReturnButton()).getElement());
        btnDisconnect.getElement().appendChild(new SVGImage(resources.disconnectButton()).getElement());
        btnRemoveAllBreakpoints.getElement().appendChild(new SVGImage(resources.removeAllBreakpointsButton()).getElement());
        btnChangeValue.getElement().appendChild(new SVGImage(resources.changeVariableValue()).getElement());
        btnEvaluateExpression.getElement().appendChild(new SVGImage(resources.evaluate()).getElement());

        TableElement breakPointsElement = Elements.createTableElement();
        breakPointsElement.setAttribute("style", "width: 100%");
        SimpleList.ListEventDelegate<Breakpoint> breakpointListEventDelegate = new SimpleList.ListEventDelegate<Breakpoint>() {
            public void onListItemClicked(Element itemElement, Breakpoint itemData) {
                breakpoints.getSelectionModel().setSelectedItem(itemData);
            }

            public void onListItemDoubleClicked(Element listItemBase, Breakpoint itemData) {
                // TODO: implement 'go to breakpoint source' feature
            }
        };

        SimpleList.ListItemRenderer<Breakpoint> breakpointListItemRenderer = new
                SimpleList.ListItemRenderer<Breakpoint>() {
                    @Override
                    public void render(Element itemElement, Breakpoint itemData) {
                        TableCellElement label = Elements.createTDElement();

                        SafeHtmlBuilder sb = new SafeHtmlBuilder();
                        // Add icon
                        sb.appendHtmlConstant("<table><tr><td>");
                        ImageResource icon = res.breakpoint();
                        if (icon != null) {
                            sb.appendHtmlConstant("<img src=\"" + icon.getSafeUri().asString() + "\">");
                        }
                        sb.appendHtmlConstant("</td>");

                        // Add title
                        sb.appendHtmlConstant("<td>");
                        sb.appendEscaped(itemData.getPath() + " - [line: " + String.valueOf(itemData.getLineNumber() + 1) + "]");
                        sb.appendHtmlConstant("</td></tr></table>");

                        label.setInnerHTML(sb.toSafeHtml().asString());

                        itemElement.appendChild(label);
                    }

                    @Override
                    public Element createElement() {
                        return Elements.createTRElement();
                    }
                };

        breakpoints = SimpleList.create((SimpleList.View)breakPointsElement, coreRes.defaultSimpleListCss(), breakpointListItemRenderer,
                                        breakpointListEventDelegate);
        this.breakpointsPanel.add(breakpoints);
        this.variables = Tree.create(rendererResources, new VariableNodeDataAdapter(), new VariableTreeNodeRenderer(rendererResources));
        this.variables.setTreeEventHandler(new Tree.Listener<Variable>() {
            @Override
            public void onNodeAction(TreeNodeElement<Variable> node) {
            }

            @Override
            public void onNodeClosed(TreeNodeElement<Variable> node) {
                selectedVariable = null;
            }

            @Override
            public void onNodeContextMenu(int mouseX, int mouseY, TreeNodeElement<Variable> node) {
            }

            @Override
            public void onNodeDragStart(TreeNodeElement<Variable> node, MouseEvent event) {
            }

            @Override
            public void onNodeDragDrop(TreeNodeElement<Variable> node, MouseEvent event) {
            }

            @Override
            public void onNodeExpanded(final TreeNodeElement<Variable> node) {
                selectedVariable = node;
                delegate.onSelectedVariableElement(selectedVariable.getData());
                delegate.onExpandVariablesTree();
            }

            @Override
            public void onNodeSelected(TreeNodeElement<Variable> node, SignalEvent event) {
                selectedVariable = node;
                if (node != null) {
                    delegate.onSelectedVariableElement(selectedVariable.getData());
                }
            }

            @Override
            public void onRootContextMenu(int mouseX, int mouseY) {
            }

            @Override
            public void onRootDragDrop(MouseEvent event) {
            }

            @Override
            public void onKeyboard(KeyboardEvent event) {
            }
        });

        this.variablesPanel.add(variables);
        minimizeButton.ensureDebugId("debugger-minimizeBut");
    }

    /** {@inheritDoc} */
    @Override
    public void setExecutionPoint(boolean existInformation, Location location) {
        StringBuilder labelText = new StringBuilder();
        if (location != null) {
            labelText.append("{" + location.getClassName() + ":" + location.getLineNumber() + "} ");
        }
        if (existInformation) {
            executionPoint.getElement().setClassName(coreRes.coreCss().defaultFont());
        } else {
            labelText.append(locale.absentInformationVariables());
            executionPoint.getElement().setClassName(coreRes.coreCss().warningFont());
        }
        executionPoint.setText(labelText.toString());
    }

    /** {@inheritDoc} */
    @Override
    public void setVariables(@Nonnull List<Variable> variables) {
        Variable root = this.variables.getModel().getRoot();
        if (root == null) {
            root = dtoFactory.createDto(Variable.class);
            this.variables.getModel().setRoot(root);
        }
        root.setVariables(variables);
        this.variables.renderTree(0);
    }

    /** {@inheritDoc} */
    @Override
    public void setBreakpoints(@Nonnull List<Breakpoint> breakpoints) {
        this.breakpoints.render(breakpoints);
    }

    /** {@inheritDoc} */
    @Override
    public void setVMName(@Nonnull String name) {
        vmName.setText(name);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableResumeButton(boolean isEnable) {
        btnResume.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableRemoveAllBreakpointsButton(boolean isEnable) {
        btnRemoveAllBreakpoints.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableDisconnectButton(boolean isEnable) {
        btnDisconnect.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableStepIntoButton(boolean isEnable) {
        btnStepInto.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public boolean resetStepIntoButton(boolean state) {
        return setButtonState(btnStepInto, state);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableStepOverButton(boolean isEnable) {
        btnStepOver.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public boolean resetStepOverButton(boolean state) {
        return setButtonState(btnStepOver, state);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableStepReturnButton(boolean isEnable) {
        btnStepReturn.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public boolean resetStepReturnButton(boolean state) {
        return setButtonState(btnStepReturn, state);
    }

    /** {@inheritDoc} */
    @Override
    public boolean setButtonState(ToggleButton button, boolean state) {
        if (state) {
            if (!button.isDown()) return true;
            button.setDown(false);
        } else {
            if (button.isDown()) return true;
            button.setDown(true);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableChangeValueButtonEnable(boolean isEnable) {
        btnChangeValue.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableEvaluateExpressionButtonEnable(boolean isEnable) {
        btnEvaluateExpression.setEnabled(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void updateSelectedVariable() {
        variables.closeNode(selectedVariable);
        variables.expandNode(selectedVariable);
    }

    /** {@inheritDoc} */
    @Override
    public void setVariablesIntoSelectedVariable(@Nonnull List<Variable> variables) {
        Variable rootVariable = selectedVariable.getData();
        rootVariable.setVariables(variables);
    }

    @UiHandler("btnResume")
    public void onResumeButtonClicked(ClickEvent event) {
        delegate.onResumeButtonClicked();
    }

    @UiHandler("btnStepInto")
    public void onStepIntoButtonClicked(ClickEvent event) {
        delegate.onStepIntoButtonClicked();
    }

    @UiHandler("btnStepOver")
    public void onStepOverButtonClicked(ClickEvent event) {
        delegate.onStepOverButtonClicked();
    }

    @UiHandler("btnStepReturn")
    public void onStepReturnButtonClicked(ClickEvent event) {
        delegate.onStepReturnButtonClicked();
    }

    @UiHandler("btnDisconnect")
    public void onDisconnectButtonClicked(ClickEvent event) {
        delegate.onDisconnectButtonClicked();
    }

    @UiHandler("btnRemoveAllBreakpoints")
    public void onRemoveAllBreakpointsButtonClicked(ClickEvent event) {
        delegate.onRemoveAllBreakpointsButtonClicked();
    }

    @UiHandler("btnChangeValue")
    public void onChangeValueButtonClicked(ClickEvent event) {
        delegate.onChangeValueButtonClicked();
    }

    @UiHandler("btnEvaluateExpression")
    public void onEvaluateExpressionButtonClicked(ClickEvent event) {
        delegate.onEvaluateExpressionButtonClicked();
    }

}
