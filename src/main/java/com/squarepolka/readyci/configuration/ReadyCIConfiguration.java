package com.squarepolka.readyci.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.squarepolka.readyci.configuration.parameter.ParsedParameter;
import com.squarepolka.readyci.configuration.parameter.ParameterParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the parsing and storage of multiple pipeline configurations
 */
public class ReadyCIConfiguration {

    public static final String ARG_SERVER = "server";
    public static final String ARG_PIPELINE = "pipeline=";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadyCIConfiguration.class);
    private static ReadyCIConfiguration instance;

    public String instanceName;
    public boolean isServerMode;
    public List<PipelineConfiguration> pipelines;
    public PipelineConfiguration pipelineToRun;

    public static ReadyCIConfiguration instance() {
        if (null == instance) {
            instance = new ReadyCIConfiguration();
        }
        return instance;
    }

    private ReadyCIConfiguration() {
        this.instanceName = "Ready CI";
        this.isServerMode = false;
        this.pipelines = new ArrayList<PipelineConfiguration>();
        this.pipelineToRun = null;
    }

    /**
     * Use the specified pipelineName to return a pipeline configuration.
     * @param pipelineName
     * @return the associated pipeline
     */
    public PipelineConfiguration getPipeline(String pipelineName) {
        for (PipelineConfiguration pipeline : pipelines) {
            if (pipeline.name.equalsIgnoreCase(pipelineName)) {
                return pipeline;
            }
        }
        throw new LoadConfigurationException(String.format("A pipeline configuration for the pipeline name, %s, does not exist", pipelineName));
    }

    public List<PipelineConfiguration> getPipelines(String repositoryName, String branch) {
        List<PipelineConfiguration> matchedPipelines = new ArrayList<PipelineConfiguration>();
        for (PipelineConfiguration pipeline : pipelines) {
            if (pipeline.matchesRepositoryName(repositoryName, branch)) {
                matchedPipelines.add(pipeline);
            }
        }
        return matchedPipelines;
    }

    public void handleInputArguments(String[] arguments) {
        for (String argument : arguments) {
            handleInputArgument(argument);
        }
    }

    private void handleInputArgument(String argument) {
        if (argument.contains(".yml")) {
            loadConfiguration(argument);
        } else if (argument.equalsIgnoreCase(ARG_SERVER)) {
            isServerMode = true;
        } else if (argument.startsWith(ARG_PIPELINE)) {
            customisePipelineToRun(argument);
        } else {
            addParameterToAllPipelines(argument);
        }
    }

    public static ReadyCIConfiguration readConfigurationFile(String configurationFileName) {
        File configurationFile = new File(configurationFileName);
        return readConfigurationFile(configurationFile);
    }

    public static ReadyCIConfiguration readConfigurationFile(File configurationFile) {
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        try {
            ReadyCIConfiguration parsedConfiguration = mapper.readValue(configurationFile, ReadyCIConfiguration.class);
            return parsedConfiguration;
        } catch (Exception e) {
            LoadConfigurationException configurationException = new LoadConfigurationException(String.format("Could not load configuration from %s: %s", configurationFile.getAbsolutePath(), e.toString()));
            configurationException.setStackTrace(e.getStackTrace());
            throw configurationException;
        }
    }

    private void loadConfiguration(String fileName) {
            ReadyCIConfiguration newConfiguration = readConfigurationFile(fileName);
            this.instanceName = newConfiguration.instanceName;
            this.isServerMode = newConfiguration.isServerMode;
            this.pipelines = newConfiguration.pipelines;
            LOGGER.info(String.format("Loaded configuration %s with %s pipelines", fileName, pipelines.size()));
    }

    private void customisePipelineToRun(String pipelineNameArgument) {
        ParsedParameter parsedParameter = new ParsedParameter(pipelineNameArgument);
        String pipelineToRunName = parsedParameter.parameterValue;
        PipelineConfiguration pipelineConfigurationToRun;
        try {
            pipelineConfigurationToRun = getPipeline(pipelineToRunName);
        } catch (LoadConfigurationException e) {
            pipelineConfigurationToRun = new PipelineConfiguration();
            pipelineConfigurationToRun.name = pipelineToRunName;
            pipelines.add(pipelineConfigurationToRun);
        }
        pipelineToRun = pipelineConfigurationToRun;
    }

    private void addParameterToAllPipelines(String parameterArgument) {
        try {
            ParsedParameter parsedParameter = new ParsedParameter(parameterArgument);
            for (PipelineConfiguration pipelineConfiguration : pipelines) {
                pipelineConfiguration.parameters.put(parsedParameter.parameterKey, parsedParameter.parameterValue);
            }
        } catch (ParameterParseException e){
            LOGGER.warn(e.toString());
        }
    }

}
