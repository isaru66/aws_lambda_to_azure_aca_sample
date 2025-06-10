resource "null_resource" "build" {
   triggers = {
    source_code_hash = "${base64sha256(join("", [for f in fileset(local.source_dir, "src/**/*.java"): filebase64("${local.source_dir}/${f}")]))}"
  }

  provisioner "local-exec" {
    
    working_dir = local.source_dir
      command = "mvn package -f pom.xml"
  }
}

locals {
    lambda_payload_filename = "${local.source_dir}/target/example-java-1.0-SNAPSHOT.jar" //example-java-1.0-SNAPSHOT.jar
    source_dir = "./aws-lambda-java"
}