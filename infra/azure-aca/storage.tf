/*
resource "azurerm_storage_account" "example" {
  name                     = "stisaru66acatrigger"
  resource_group_name      = data.azurerm_resource_group.main.name
  location                 = data.azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"
}

resource "azurerm_storage_container" "blobinput" {
  name                  = "blobinput"
  storage_account_id    = azurerm_storage_account.example.id
  container_access_type = "private"
}
*/