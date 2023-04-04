from kpops.components import KubernetesApp
from kpops.components.base_components.kubernetes_app import KubernetesAppConfig

class RestService(KubernetesApp):
    type = "rest-service"
    app: KubernetesAppConfig
    repoConfig: None = None

    def get_helm_chart(self) -> str:
        return "./charts/rest-service"
