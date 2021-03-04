package com.kickstarter.libs

import com.kickstarter.libs.models.OptimizelyEnvironment
import com.kickstarter.libs.models.OptimizelyExperiment
import com.kickstarter.libs.models.OptimizelyFeature
import com.kickstarter.libs.utils.ExperimentData
import com.kickstarter.libs.utils.ExperimentUtils
import com.kickstarter.models.User
import org.json.JSONArray
import org.json.JSONObject

interface ExperimentsClientType {

    fun ExperimentsClientType.attributes(experimentData: ExperimentData, optimizelyEnvironment: OptimizelyEnvironment): Map<String, *> {
        return ExperimentUtils.attributes(experimentData, appVersion(), OSVersion(), optimizelyEnvironment)
    }

    /**
     * Map with all the data available for a concrete Experiment
     * TODO: method to be deleted on https://kickstarter.atlassian.net/browse/EP-187
     */
    fun optimizelyProperties(experimentData: ExperimentData): Map<String, Any> {
        val experiments = JSONArray()
        val properties = mapOf("optimizely_api_key" to optimizelyEnvironment().sdkKey,
                "optimizely_environment_key" to optimizelyEnvironment().environmentKey,
                "optimizely_experiments" to experiments)

        for (experiment in OptimizelyExperiment.Key.values()) {
            val variation = trackingVariation(experiment.key, experimentData) ?: "unknown"
            experiments.put(JSONObject(mutableMapOf<Any?, Any?>("optimizely_experiment_slug" to experiment.key,
                    "optimizely_variant_id" to variation)))
        }

        return properties
    }

    fun appVersion(): String
    fun enabledFeatures(user: User?): List<String>
    fun isFeatureEnabled(feature: OptimizelyFeature.Key, experimentData: ExperimentData): Boolean
    fun optimizelyEnvironment(): OptimizelyEnvironment
    fun OSVersion(): String
    fun track(eventKey: String, experimentData: ExperimentData)
    fun trackingVariation(experimentKey: String, experimentData: ExperimentData): String?
    fun userId() : String
    fun variant(experiment: OptimizelyExperiment.Key, experimentData: ExperimentData): OptimizelyExperiment.Variant?

    /**
     * Map with all the experiments available to the app, with the corresponding variant:
     * "session_variants_optimizely" : [ "Experiment1":"variant1",
     *                                   "Experiment2":"varian2"]
     */
    fun getTrackingProperties(): Map<String, JSONArray>
}

const val EXPERIMENTS_CLIENT_READY = "experiments_client_ready"
