# resource "azurerm_resource_group" "example" {
#   name     = "${var.resource_prefix}-resources"
#   location = "Southeast Asia"
# }

data "azurerm_resource_group" "main" {
  name = var.resource_group_name
}

resource "azurerm_log_analytics_workspace" "example" {
  name                = "log-${var.resource_prefix}-aca-01"
  location            = data.azurerm_resource_group.main.location
  resource_group_name = data.azurerm_resource_group.main.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

resource "azurerm_container_app_environment" "example" {
  name                       = "acae-001"
  location                   = data.azurerm_resource_group.main.location
  resource_group_name        = data.azurerm_resource_group.main.name
  log_analytics_workspace_id = azurerm_log_analytics_workspace.example.id
}

resource "azurerm_container_app" "aca-eventgrid" {
  name                         = "aca-${var.resource_prefix}-app"
  container_app_environment_id = azurerm_container_app_environment.example.id
  resource_group_name          = data.azurerm_resource_group.main.name
  revision_mode                = "Single"

  template {
    # Sample Application
    # container {
    #   name   = "sampleapp"
    #   image  = "mcr.microsoft.com/k8se/quickstart:latest"
    #   cpu    = 0.25
    #   memory = "0.5Gi"
    # }
    min_replicas = 0
    max_replicas = 10
    container {
      name   = "azure-aca-java-eventgrid"
      image  = "isaru66/azure-aca-java-eventgrid:latest"
      cpu    = 0.5
      memory = "1Gi"
      env {
        name  = "DEST_BUCKET"
        value = "blobinput"
      }
      env {
        name  = "AZURE_BLOB_ENDPOINT"
        value = "https://stisaru66acatrigger.blob.core.windows.net/blobinput"
      }
      env {
        name  = "AZURE_CLIENT_ID"
        value = azurerm_user_assigned_identity.aca_eventgrid.client_id
      }
    }
    http_scale_rule {
      concurrent_requests = "10"
      name                = "http-scaler"
    }
  }
  ingress {
    allow_insecure_connections = false
    external_enabled           = true
    target_port                = 8080
    transport                  = "auto"
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }
  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.aca_eventgrid.id]
  }
}
resource "azurerm_user_assigned_identity" "aca_eventgrid" {
  name                = "umi-${var.resource_prefix}-aca-eventgrid"
  resource_group_name = data.azurerm_resource_group.main.name
  location            = data.azurerm_resource_group.main.location
}

resource "azurerm_container_registry" "example" {
  name                = "acr${var.resource_prefix}"
  resource_group_name = data.azurerm_resource_group.main.name
  location            = data.azurerm_resource_group.main.location
  sku                 = "Basic"
  admin_enabled       = true
}

data "azurerm_storage_account" "example" {
  name                = "stisaru66acatrigger"
  resource_group_name = data.azurerm_resource_group.main.name
}

resource "azurerm_role_assignment" "acr_pull" {
  scope                = azurerm_container_registry.example.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_user_assigned_identity.aca_eventgrid.principal_id
}


resource "azurerm_role_assignment" "contributor" {
  scope                = data.azurerm_storage_account.example.id
  role_definition_name = "Contributor"
  principal_id         = azurerm_user_assigned_identity.aca_eventgrid.principal_id
}

resource "azurerm_role_assignment" "blob_contributor" {
  scope                = data.azurerm_storage_account.example.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.aca_eventgrid.principal_id
}