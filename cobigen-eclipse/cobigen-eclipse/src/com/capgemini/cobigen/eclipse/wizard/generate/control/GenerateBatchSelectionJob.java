package com.capgemini.cobigen.eclipse.wizard.generate.control;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.capgemini.cobigen.api.to.GenerationReportTo;
import com.capgemini.cobigen.api.to.TemplateTo;
import com.capgemini.cobigen.eclipse.generator.CobiGenWrapper;

/**
 * Running this process as issued in {@link IRunnableWithProgress} performs the generation tasks of the
 * generation wizard for each selected pojo.
 */
public class GenerateBatchSelectionJob extends AbstractGenerateSelectionJob {

    /** {@link List} containing the types of the selected inputs */
    private List<IType> inputTypes;

    /** {@link IPackageFragment}, which should be the input for the generation process */
    private IPackageFragment container;

    /**
     * Creates a new process ({@link IRunnableWithProgress}) for performing the generation tasks
     * @param javaGeneratorWrapper
     *            with which to generate the contents
     * @param templatesToBeGenerated
     *            {@link Set} of template ids to be generated
     * @param inputTypes
     *            {@link List} containing the types of the selected pojos
     */
    public GenerateBatchSelectionJob(CobiGenWrapper javaGeneratorWrapper, List<TemplateTo> templatesToBeGenerated,
        List<IType> inputTypes) {

        super(javaGeneratorWrapper, templatesToBeGenerated);
        this.inputTypes = inputTypes;
    }

    /**
     * Creates a new process ({@link IRunnableWithProgress}) for performing the generation tasks
     * @param javaGeneratorWrapper
     *            with which to generate the contents
     * @param templatesToBeGenerated
     *            {@link Set} of template ids to be generated
     * @param container
     *            selected {@link IPackageFragment} for the generation
     */
    public GenerateBatchSelectionJob(CobiGenWrapper javaGeneratorWrapper, List<TemplateTo> templatesToBeGenerated,
        IPackageFragment container) {

        super(javaGeneratorWrapper, templatesToBeGenerated);
        this.container = container;
    }

    @Override
    protected GenerationReportTo performGeneration(IProgressMonitor monitor) throws Exception {
        LOG.info("Perform generation of contents in batch mode...");

        GenerationReportTo result;
        if (inputTypes != null && inputTypes.size() == 0 && container == null) {
            LOG.warn("Generation finished: No inputs provided!");
            result = new GenerationReportTo();
            result.addWarning("No input provided!");
            return result;
        }

        return cobigenWrapper.generate(templatesToBeGenerated, monitor);
    }
}