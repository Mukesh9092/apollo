package io.logz.apollo.helpers;

import io.logz.apollo.auth.User;
import io.logz.apollo.dao.DeployableVersionDao;
import io.logz.apollo.dao.DeploymentDao;
import io.logz.apollo.dao.EnvironmentDao;
import io.logz.apollo.dao.ServiceDao;
import io.logz.apollo.dao.UserDao;
import io.logz.apollo.models.DeployableVersion;
import io.logz.apollo.models.Deployment;
import io.logz.apollo.models.Environment;
import io.logz.apollo.models.Service;

/**
 * Created by roiravhon on 2/7/17.
 */
public class RealDeploymentGenerator {

    private static final int NODE_PORT = 30002;
    private final String DEFAULT_LABEL_KEY = "app";
    private final String DEFAULT_LABEL_VALUE = "nginx";

    private final String DEFAULT_ENVIRONMENT_VARIABLE_NAME = "ENV";
    private final String DEFAULT_ENVIRONMENT_VARIABLE_VALUE = "enval";

    private final StandaloneApollo standaloneApollo;
    private final Environment environment;
    private final Service service;
    private final DeployableVersion deployableVersion;
    private final User user;
    private final Deployment deployment;

    public RealDeploymentGenerator(String deploymentImageName, String extraLabelKey, String extraLabelValue, int servicePortCoefficient) {
        this(deploymentImageName, extraLabelKey, extraLabelValue, servicePortCoefficient, null);
    }

    public RealDeploymentGenerator(String deploymentImageName, String extraLabelKey, String extraLabelValue, int servicePortCoefficient, String deploymentParams) {
        try {
            standaloneApollo = StandaloneApollo.getOrCreateServer();
            DeploymentDao deploymentDao = standaloneApollo.getInstance(DeploymentDao.class);
            EnvironmentDao environmentDao = standaloneApollo.getInstance(EnvironmentDao.class);
            ServiceDao serviceDao = standaloneApollo.getInstance(ServiceDao.class);
            DeployableVersionDao deployableVersionDao = standaloneApollo.getInstance(DeployableVersionDao.class);
            UserDao userDao = standaloneApollo.getInstance(UserDao.class);

            // Create all models in DB
            environment = ModelsGenerator.createEnvironment();
            environment.setServicePortCoefficient(servicePortCoefficient);
            environmentDao.addEnvironment(environment);

            service = ModelsGenerator.createService();
            service.setDeploymentYaml(getDeploymentKubernetesYaml(deploymentImageName, extraLabelKey, extraLabelValue));
            service.setServiceYaml(getServiceDeploymentYaml(extraLabelKey, extraLabelValue));
            serviceDao.addService(service);

            deployableVersion = ModelsGenerator.createDeployableVersion(service);
            deployableVersionDao.addDeployableVersion(deployableVersion);

            user = ModelsGenerator.createRegularUser();
            userDao.addUser(user);

            deployment = ModelsGenerator.createDeployment(service, environment, deployableVersion);
            deployment.setStatus(Deployment.DeploymentStatus.PENDING);
            deployment.setSourceVersion("abc" + Common.randomStr(5));
            deployment.setUserEmail(user.getUserEmail());
            deployment.setDeploymentParams(deploymentParams);
            deploymentDao.addDeployment(deployment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getDefaultLabelKey() {
        return DEFAULT_LABEL_KEY;
    }

    public String getDefaultLabelValue() {
        return DEFAULT_LABEL_VALUE;
    }

    public String getDefaultEnvironmentVariableName() {
        return DEFAULT_ENVIRONMENT_VARIABLE_NAME;
    }

    public String getDefaultEnvironmentVariableValue() {
        return DEFAULT_ENVIRONMENT_VARIABLE_VALUE;
    }

    public int getDefaultNodePort() {
        return NODE_PORT;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Service getService() {
        return service;
    }

    public DeployableVersion getDeployableVersion() {
        return deployableVersion;
    }

    public User getUser() {
        return user;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void updateDeploymentStatus(Deployment.DeploymentStatus deploymentStatus) {
        DeploymentDao deploymentDao = standaloneApollo.getInstance(DeploymentDao.class);
        deploymentDao.updateDeploymentStatus(deployment.getId(), deploymentStatus);
    }

    private String getDeploymentKubernetesYaml(String imageName, String extraLabelKey, String extraLabelValue) {

        return "apiVersion: extensions/v1beta1\n" +
                "kind: Deployment\n" +
                "metadata:\n" +
                "  labels:\n" +
                "    tahat: nginx\n" +
                "    " + DEFAULT_LABEL_KEY + ": " + DEFAULT_LABEL_VALUE + "\n" +
                "    " + extraLabelKey + ": " + extraLabelValue + "\n" +
                "  name: nginx\n" +
                "  namespace: default\n" +
                "spec:\n" +
                "  replicas: 1\n" +
                "  strategy:\n" +
                "    rollingUpdate:\n" +
                "      maxSurge: 1\n" +
                "      maxUnavailable: 0\n" +
                "    type: RollingUpdate\n" +
                "  template:\n" +
                "    metadata:\n" +
                "      labels:\n" +
                "        pod: label\n" +
                "    spec:\n" +
                "      containers:\n" +
                "      - image: " + imageName + "\n" +
                "        imagePullPolicy: Always\n" +
                "        name: roi-apollo-test\n" +
                "        env:\n" +
                "        -  name: " + DEFAULT_ENVIRONMENT_VARIABLE_NAME + "\n" +
                "           value: " + DEFAULT_ENVIRONMENT_VARIABLE_VALUE + "\n" +
                "        ports:\n" +
                "        - containerPort: 80\n" +
                "          protocol: TCP\n" +
                "        resources: {}\n" +
                "      dnsPolicy: ClusterFirst\n" +
                "      restartPolicy: Always\n" +
                "      securityContext: {}\n" +
                "      terminationGracePeriodSeconds: 30";
    }

    private String getServiceDeploymentYaml(String extraLabelKey, String extraLabelValue) {
        return "apiVersion: v1\n" +
                "kind: Service\n" +
                "metadata:\n" +
                "  labels:\n" +
                "    " + DEFAULT_LABEL_KEY + ": " + DEFAULT_LABEL_VALUE + "\n" +
                "    " + extraLabelKey + ": " + extraLabelValue + "\n" +
                "  name: roi-test-service\n" +
                "  namespace: default\n" +
                "spec:  \n" +
                "  ports:\n" +
                "  - nodePort: " + NODE_PORT + "\n" +
                "    port: 80\n" +
                "    protocol: TCP\n" +
                "    targetPort: 80\n" +
                "  selector:\n" +
                "    app: nginx\n" +
                "  sessionAffinity: None\n" +
                "  type: NodePort\n" +
                "status:\n" +
                "  loadBalancer: {}";
    }
}
