variable "keycloak_url" {
  description = "Base URL of the Keycloak instance"
  type        = string
  default     = "http://localhost:8081"
}

variable "keycloak_admin_user" {
  description = "Keycloak admin username"
  type        = string
  default     = "admin"
}

variable "keycloak_admin_password" {
  description = "Keycloak admin password"
  type        = string
  default     = "admin"
  sensitive   = true
}
