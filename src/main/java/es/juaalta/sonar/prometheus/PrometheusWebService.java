package es.juaalta.sonar.prometheus;

import static java.util.Objects.nonNull;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.SearchRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;

import es.juaalta.sonar.prometheus.utils.ConvertUtils;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * @author juaalta
 *
 */
@SuppressWarnings("deprecation")
public class PrometheusWebService implements WebService {

  static final Set<Metric<?>> SUPPORTED_METRICS = new HashSet<>();

  static final String CONFIG_PREFIX = "prometheus.export.";

  private static final String METRIC_PREFIX = "sonarqube_";

  private final Configuration configuration;

  private final Map<String, Gauge> gauges = new HashMap<>();

  private final Set<Metric<?>> enabledMetrics = new HashSet<>();

  static {

    CoreMetrics.getMetrics().forEach(x -> {
      // Not insert deprecated metrics
      if (!(x.getKey().equals(CoreMetrics.QUALITY_PROFILES_KEY)) && !(x.getKey().equals(CoreMetrics.DIRECTORIES_KEY))
          && !(x.getKey().equals(CoreMetrics.PUBLIC_API_KEY))
          && !(x.getKey().equals(CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY))
          && !(x.getKey().equals(CoreMetrics.PUBLIC_UNDOCUMENTED_API_KEY))
          && !(x.getKey().equals(CoreMetrics.FILE_COMPLEXITY_KEY))
          && !(x.getKey().equals(CoreMetrics.COMPLEXITY_IN_CLASSES_KEY))
          && !(x.getKey().equals(CoreMetrics.CLASS_COMPLEXITY_KEY))
          && !(x.getKey().equals(CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY))
          && !(x.getKey().equals(CoreMetrics.FUNCTION_COMPLEXITY_KEY))
          && !(x.getKey().equals(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY))
          && !(x.getKey().equals(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY))
          && !(x.getKey().equals(CoreMetrics.DUPLICATIONS_DATA_KEY))
          && !(x.getKey().equals(CoreMetrics.COMMENT_LINES_DATA_KEY))) {

        // Erased for failing to output Prometheus, empty description
        if (x.getDescription() != null) {
          SUPPORTED_METRICS.add(x);
        }
      }
    });

  }

  private static final Logger LOGGER = Loggers.get(PrometheusWebService.class.getName());

  /**
   * @param configuration Configuration
   */
  public PrometheusWebService(Configuration configuration) {

    this.configuration = configuration;
  }

  @Override
  public void define(Context context) {

    updateEnabledMetrics();
    updateEnabledGauges();

    NewController controller = context.createController("api/prometheus");
    controller.setDescription("Prometheus Exporter");

    controller.createAction("metrics").setHandler((request, response) -> {

      updateEnabledMetrics();
      updateEnabledGauges();

      if (!this.enabledMetrics.isEmpty()) {

        WsClient wsClient = WsClientFactories.getLocal().newClient(request.localConnector());

        LOGGER.info("Start of processing");

        List<Components.Component> projects = getProjects(wsClient);
        projects.forEach(project -> {

          Measures.ComponentWsResponse wsResponse = getMeasures(wsClient, project);

          wsResponse.getComponent().getMeasuresList().forEach(measure -> {
            try {

              if (this.gauges.containsKey(measure.getMetric())) {

                // LOGGER.info("Metric data ************");
                // LOGGER.info("metric: " + measure.getMetric());
                // LOGGER.info("fields: " + measure.getAllFields());

                Metric obtainedMetric = CoreMetrics.getMetric(measure.getMetric());

                LOGGER.info("type: " + obtainedMetric.getType().toString());
                // LOGGER.info("is numeric type: " + obtainedMetric.isNumericType());
                // LOGGER.info("is Percentage type: " + obtainedMetric.isPercentageType());
                // LOGGER.info("is data type: " + obtainedMetric.isDataType());
                // LOGGER.info("is optimized best value: " + obtainedMetric.isOptimizedBestValue());

                if (obtainedMetric.isNumericType() || obtainedMetric.isPercentageType()) {
                  this.gauges.get(measure.getMetric())
                      .labels(project.getKey(), project.getName(), obtainedMetric.getDomain())
                      .set(ConvertUtils.getDoubleValue(measure.getValue()));
                } else if (obtainedMetric.isDataType()) {
                  LOGGER.info(measure.getMetric());
                  LOGGER.info(obtainedMetric.getType().toString());
                  LOGGER.info(measure.getValue());
                  this.gauges.get(measure.getMetric())
                      .labels(project.getKey(), project.getName(), obtainedMetric.getDomain()).set(0);
                } else {

                  switch (obtainedMetric.getType()) {
                    case LEVEL:
                      this.gauges.get(measure.getMetric())
                          .labels(project.getKey(), project.getName(), obtainedMetric.getDomain(), measure.getValue())
                          .set(0);
                      break;
                    case STRING:
                      Double value = 0.0;
                      if (ConvertUtils.isNumeric(measure.getValue())) {
                        value = ConvertUtils.getDoubleValue(measure.getValue());
                      }
                      this.gauges.get(measure.getMetric())
                          .labels(project.getKey(), project.getName(), obtainedMetric.getDomain(), measure.getValue())
                          .set(value);
                      break;
                    default:
                      LOGGER.info(measure.getMetric());
                      LOGGER.info(obtainedMetric.getType().toString());
                      LOGGER.info(measure.getValue());
                      LOGGER.info("DEFAULT #######");
                      this.gauges.get(measure.getMetric())
                          .labels(project.getKey(), project.getName(), obtainedMetric.getDomain())
                          .set(ConvertUtils.getDoubleValue(measure.getValue()));
                      break;
                  }
                }

              }

            } catch (Exception e) {
              LOGGER.error("Error processing metric: " + measure.getMetric(), e);
            }
          });
        });
      }

      LOGGER.info("End of processing");

      OutputStream output = response.stream().setMediaType(TextFormat.CONTENT_TYPE_004).setStatus(200).output();

      try (OutputStreamWriter writer = new OutputStreamWriter(output)) {

        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());

      } catch (Exception e) {
        LOGGER.error("Error writing: ", e);
      }

    });

    controller.done();

  }

  private void updateEnabledMetrics() {

    Map<Boolean, List<Metric<?>>> byEnabledState = SUPPORTED_METRICS.stream().collect(
        Collectors.groupingBy(metric -> this.configuration.getBoolean(CONFIG_PREFIX + metric.getKey()).orElse(false)));

    this.enabledMetrics.clear();

    if (nonNull(byEnabledState.get(true))) {
      this.enabledMetrics.addAll(byEnabledState.get(true));
    }
  }

  private void updateEnabledGauges() {

    CollectorRegistry.defaultRegistry.clear();

    this.enabledMetrics.forEach(metric -> {

      if (metric.isNumericType() || metric.isPercentageType()) {
        this.gauges.put(metric.getKey(), Gauge.build().name(METRIC_PREFIX + metric.getKey())
            .help(metric.getDescription()).labelNames("key", "name", "domain").register());
      } else if (metric.isDataType()) {
        this.gauges.put(metric.getKey(), Gauge.build().name(METRIC_PREFIX + metric.getKey())
            .help(metric.getDescription()).labelNames("key", "name", "domain").register());
      } else {

        switch (metric.getType()) {
          case LEVEL:
            this.gauges.put(metric.getKey(), Gauge.build().name(METRIC_PREFIX + metric.getKey())
                .help(metric.getDescription()).labelNames("key", "name", "domain", "level").register());
            break;
          case STRING:
            this.gauges.put(metric.getKey(), Gauge.build().name(METRIC_PREFIX + metric.getKey())
                .help(metric.getDescription()).labelNames("key", "name", "domain", "value").register());
            break;
          default:
            this.gauges.put(metric.getKey(), Gauge.build().name(METRIC_PREFIX + metric.getKey())
                .help(metric.getDescription()).labelNames("key", "name", "domain").register());
            break;
        }

      }

    });
  }

  private Measures.ComponentWsResponse getMeasures(WsClient wsClient, Components.Component project) {

    List<String> metricKeys = this.enabledMetrics.stream().map(Metric::getKey).collect(Collectors.toList());

    return wsClient.measures()
        .component(new ComponentRequest().setComponent(project.getKey()).setMetricKeys(metricKeys));
  }

  private List<Components.Component> getProjects(WsClient wsClient) {

    return wsClient.components()
        .search(new SearchRequest().setQualifiers(Collections.singletonList(Qualifiers.PROJECT)).setPs("500"))
        .getComponentsList();
  }
}
