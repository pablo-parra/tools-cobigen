/*******************************************************************************
 * Copyright © Capgemini 2013. All rights reserved.
 ******************************************************************************/
package com.capgemini.cobigen.eclipse.wizard.common.control;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import com.capgemini.cobigen.eclipse.common.tools.PathUtil;
import com.capgemini.cobigen.eclipse.generator.java.JavaGeneratorWrapper;
import com.capgemini.cobigen.eclipse.generator.java.entity.ComparableIncrement;
import com.capgemini.cobigen.eclipse.wizard.common.SelectFilesPage;
import com.capgemini.cobigen.eclipse.wizard.common.model.PackagesContentProvider;
import com.capgemini.cobigen.eclipse.wizard.common.model.SelectFileContentProvider;
import com.capgemini.cobigen.eclipse.wizard.common.model.SelectFileLabelProvider;
import com.capgemini.cobigen.eclipse.wizard.common.model.stubs.IJavaElementStub;
import com.capgemini.cobigen.eclipse.wizard.common.model.stubs.IResourceStub;
import com.capgemini.cobigen.extension.to.IncrementTo;
import com.capgemini.cobigen.extension.to.TemplateTo;
import com.google.common.collect.Lists;

/**
 * This {@link CheckStateListener} provides the check / uncheck of the generation packages list and the generation
 * resource tree and synchronizes both in order to get a consistent view
 * 
 * @author mbrunnli (19.02.2013)
 */
public class CheckStateListener implements ICheckStateListener, SelectionListener {

    /**
     * Currently used {@link JavaGeneratorWrapper} instance
     */
    private JavaGeneratorWrapper javaGeneratorWrapper;

    /**
     * The {@link SelectFilesPage} of the wizard providing the different viewer
     */
    private SelectFilesPage page;

    /**
     * Lastly checked generation packages
     */
    private Set<Object> lastCheckedPackages = new HashSet<Object>();

    /**
     * Defines whether the {@link JavaGeneratorWrapper} is in batch mode.
     */
    private boolean batch;

    /**
     * Creates a new {@link CheckStateListener} instance
     * 
     * @param javaGeneratorWrapper currently used {@link JavaGeneratorWrapper} instance
     * @param page current {@link SelectFilesPage} reference
     * @param batch states whether the check state listener should run in batch mode
     * @author mbrunnli (25.02.2013)
     */
    public CheckStateListener(JavaGeneratorWrapper javaGeneratorWrapper, SelectFilesPage page, boolean batch) {

        this.javaGeneratorWrapper = javaGeneratorWrapper;
        this.page = page;
        this.batch = batch;
    }

    /**
     * {@inheritDoc}
     * 
     * @author mbrunnli (19.02.2013)
     */
    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {

        CheckboxTreeViewer resourcesTree = page.getResourcesTree();
        CheckboxTreeViewer packageSelector = page.getPackageSelector();
        if (event.getSource().equals(resourcesTree)) {
            resourcesTree.setSubtreeChecked(event.getElement(), event.getChecked());
            ((SelectFileLabelProvider) resourcesTree.getLabelProvider()).setCheckedResources(resourcesTree
                    .getCheckedElements());
            refreshNodes(event);
        } else if (event.getSource().equals(packageSelector)) {
            performCheckLogic(event, packageSelector);
            Set<Object> checkedElements = new HashSet<Object>(Arrays.asList(packageSelector.getCheckedElements()));
            performCheckLogicForALLPackage(packageSelector, checkedElements);

            Set<String> paths = getSelectedGenerationPaths();
            ((SelectFileContentProvider) resourcesTree.getContentProvider()).filter(paths);
            resourcesTree.refresh();
            resourcesTree.expandAll();
            if (!batch) {
                setSimulatedResourcesChecked();
                setMergeableResourcesChecked();
            } else {
                setAllResourcesChecked();
            }
        }

        checkPageComplete();
    }

    /**
     * Checks whether there are resources selected for generation and sets the {@link WizardPage} to be completed if
     * there is at least one selected resource
     * 
     * @author mbrunnli (28.04.2013)
     */
    private void checkPageComplete() {

        CheckboxTreeViewer resourcesTree = page.getResourcesTree();
        if (page.isSetRememberSelection() || resourcesTree.getCheckedElements().length > 0) {
            page.setPageComplete(true);
        } else {
            page.setPageComplete(false);
        }
    }

    /**
     * Performs an intelligent check logic such that the same element in different paths will be checked simultaneously,
     * parents will be unselected if at least one child is not selected, and parents will be automatically selected if
     * all children of the parent are selected
     * 
     * @param event triggering {@link CheckStateChangedEvent}
     * @param packageSelector current {@link CheckboxTreeViewer} for the package selection
     * @author mbrunnli (26.03.2013)
     */
    private void performCheckLogic(CheckStateChangedEvent event, CheckboxTreeViewer packageSelector) {

        PackagesContentProvider cp = (PackagesContentProvider) packageSelector.getContentProvider();
        TreePath[] paths = cp.getAllPaths(event.getElement());
        for (TreePath path : paths) {
            packageSelector.setSubtreeChecked(path, event.getChecked());
        }

        TreePath[] parents = cp.getParents(event.getElement());
        if (event.getChecked()) {
            for (TreePath parent : parents) {
                boolean allChecked = true;
                for (Object child : cp.getChildren(parent)) {
                    if (!packageSelector.getChecked(parent.createChildPath(child))) {
                        allChecked = false;
                        break;
                    }
                }
                if (allChecked) {
                    packageSelector.setChecked(parent, true);
                }
            }
        } else {
            for (TreePath parent : parents) {
                if (parent.getSegmentCount() > 0) {
                    packageSelector.setChecked(parent, false);
                }
            }
        }
    }

    /**
     * Refreshes the nodes affected by the given {@link CheckStateChangedEvent}
     * 
     * @param event {@link CheckStateChangedEvent} of {@link #checkStateChanged(CheckStateChangedEvent)}
     * @author mbrunnli (14.03.2013)
     */
    private void refreshNodes(CheckStateChangedEvent event) {

        if (event.getElement() instanceof Object[]) {
            for (Object o : (Object[]) event.getElement()) {
                page.getResourcesTree().refresh(o);
            }
        } else {
            page.getResourcesTree().refresh(event.getElement());
        }
    }

    /**
     * Returns all generation paths of the currently selected generation packages
     * 
     * @return all generation paths of the currently selected generation packages
     * @author mbrunnli (26.02.2013)
     */
    private Set<String> getSelectedGenerationPaths() {

        Set<String> paths = new HashSet<String>();
        for (Object o : lastCheckedPackages) {
            if (o instanceof ComparableIncrement) {
                ComparableIncrement pkg = ((ComparableIncrement) o);
                paths.addAll(PathUtil.createWorkspaceRelativePaths(javaGeneratorWrapper.getGenerationTargetProject(),
                        getDestinationPaths(pkg)));
                if (pkg.getId().equals("all")) {
                    break;
                }
            }
        }
        return paths;
    }

    /**
     * Returns the set of all destination paths for the templates of the given {@link ComparableIncrement}
     * 
     * @return the set of all destination paths for the templates of the given {@link ComparableIncrement}
     * @param pkg {@link ComparableIncrement} the template destination paths should be retrieved for
     * @author mbrunnli (11.03.2013)
     */
    private Set<String> getDestinationPaths(ComparableIncrement pkg) {

        Set<String> paths = new HashSet<String>();
        for (TemplateTo tmp : pkg.getTemplates()) {
            paths.add(tmp.getDestinationPath());
        }
        return paths;
    }

    /**
     * Performs an intelligent check logic, e.g. check/uncheck all packages when selecting "all"
     * 
     * @param packageSelector the {@link CheckboxTableViewer} listing all generation packages
     * @param checkedElements the {@link Set} of all elements checked by the user
     * @author mbrunnli (25.02.2013)
     */
    private void performCheckLogicForALLPackage(CheckboxTreeViewer packageSelector, Set<Object> checkedElements) {

        Set<Object> addedDiff = new HashSet<Object>(checkedElements);
        Set<Object> removedDiff = new HashSet<Object>(lastCheckedPackages);
        addedDiff.removeAll(lastCheckedPackages);
        removedDiff.removeAll(checkedElements);
        ComparableIncrement all =
                new ComparableIncrement("all", "All", null, Lists.<TemplateTo> newLinkedList(),
                        Lists.<IncrementTo> newLinkedList());
        if (!lastCheckedPackages.contains(all) && addedDiff.contains(all)) {
            selectAllPackages(packageSelector);
        } else if (lastCheckedPackages.contains(all) && removedDiff.contains(all)) {
            setAllChecked(packageSelector, false);
            lastCheckedPackages.clear();
        } else if (!removedDiff.isEmpty()) {
            lastCheckedPackages = checkedElements;
            lastCheckedPackages.remove(all);
            packageSelector.setChecked(all, false);
        } else {
            lastCheckedPackages = checkedElements;
        }
    }

    /**
     * Sets all checkboxes of the package selector to be checked
     * 
     * @param packageSelector {@link CheckboxTreeViewer}
     * @param checked <code>true</code> for all check boxes being checked, <code>false</code> otherwise
     * @author mbrunnli (26.03.2013)
     */
    private void setAllChecked(CheckboxTreeViewer packageSelector, boolean checked) {

        TreePath[] rootPaths = ((PackagesContentProvider) packageSelector.getContentProvider()).getAllRootPaths();
        for (TreePath path : rootPaths) {
            packageSelector.setSubtreeChecked(path, checked);
        }
    }

    /**
     * Selects all packages in the package selector
     * 
     * @param packageSelector package selector
     * @author mbrunnli (26.02.2013)
     */
    private void selectAllPackages(CheckboxTreeViewer packageSelector) {

        setAllChecked(packageSelector, true);
        lastCheckedPackages = new HashSet<Object>(Arrays.asList((Object[]) packageSelector.getInput()));
    }

    /**
     * Sets all simulated resources to be initially checked
     * 
     * @author mbrunnli (18.02.2013)
     */
    private void setSimulatedResourcesChecked() {

        CheckboxTreeViewer resourcesTree = page.getResourcesTree();
        LinkedList<Object> worklist =
                Lists.newLinkedList(Arrays.asList(((SelectFileContentProvider) resourcesTree.getContentProvider())
                        .getElements(resourcesTree.getInput())));

        while (worklist.peek() != null) {
            Object o = worklist.poll();
            if (o instanceof IJavaElementStub || o instanceof IResourceStub) {
                resourcesTree.setChecked(o, true);
            }
            worklist.addAll(Arrays.asList(((SelectFileContentProvider) resourcesTree.getContentProvider())
                    .getChildren(o)));
        }
    }

    /**
     * Sets all mergeable files to be checked
     * 
     * @author mbrunnli (15.03.2013)
     */
    private void setMergeableResourcesChecked() {

        CheckboxTreeViewer resourcesTree = page.getResourcesTree();
        for (IFile file : javaGeneratorWrapper.getMergeableFiles()) {
            Object mergableTreeObject =
                    ((SelectFileContentProvider) resourcesTree.getContentProvider()).getProvidedObject(file
                            .getFullPath().toString());
            if (mergableTreeObject != null) {
                resourcesTree.setChecked(mergableTreeObject, true);
            }
        }
    }

    /**
     * Sets all files to be checked
     * 
     * @author trippl (24.04.2013)
     */
    private void setAllResourcesChecked() {

        CheckboxTreeViewer resourcesTree = page.getResourcesTree();
        for (Object o : resourcesTree.getVisibleExpandedElements()) {
            resourcesTree.setChecked(o, true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @author mbrunnli (28.04.2013)
     */
    @Override
    public void widgetSelected(SelectionEvent e) {

        checkPageComplete();
    }

    /**
     * {@inheritDoc}
     * 
     * @author mbrunnli (28.04.2013)
     */
    @Override
    public void widgetDefaultSelected(SelectionEvent e) {

        checkPageComplete();
    }
}