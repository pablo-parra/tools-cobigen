/*
 * Copyright © Capgemini 2013. All rights reserved.
 */
package com.capgemini.cobigen.textmerger;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.capgemini.cobigen.extension.IMerger;

/**
 * The {@link TextAppender} allows appending the patch to the base file
 * @author mbrunnli (03.06.2014)
 */
public class TextAppender implements IMerger {

    /**
     * Type (or name) of the instance
     */
    private String type;

    /**
     * States whether the patch should be appended after adding a new line to the base file
     */
    private boolean withNewLineBeforehand;

    /**
     * Creates a new text appender which appends the patch to the base file. If {@link #withNewLineBeforehand}
     * is set, the patch will be appended by first adding a new line to the base file
     * @param type
     *            of the text appender instance
     * @param withNewLineBeforehand
     * @author mbrunnli (03.06.2014)
     */
    public TextAppender(String type, boolean withNewLineBeforehand) {
        this.type = type;
        this.withNewLineBeforehand = withNewLineBeforehand;
    }

    /**
     * {@inheritDoc}
     * @author mbrunnli (03.06.2014)
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     * @author mbrunnli (03.06.2014)
     */
    @Override
    public String merge(File base, String patch, String targetCharset) throws Exception {
        String mergedString = FileUtils.readFileToString(base, targetCharset);
        if (withNewLineBeforehand) {
            mergedString += System.lineSeparator();
        }
        mergedString += patch;
        return mergedString;
    }
}