data "aws_ami" "amazon_linux_2023" {
  count = var.ami_id == "" ? 1 : 0

  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_launch_template" "this" {
  name_prefix   = "${var.launch_template_name}-"
  image_id      = var.ami_id != "" ? var.ami_id : data.aws_ami.amazon_linux_2023[0].id
  instance_type = var.instance_type

  iam_instance_profile {
    name = var.instance_profile_name
  }

  vpc_security_group_ids = [var.ec2_sg_id]

  user_data = base64encode(templatefile("${path.module}/user_data.sh.tpl", {
    app_name        = var.app_name
    environment     = var.environment
    artifact_bucket = var.artifact_bucket
    artifact_prefix = var.artifact_prefix
    release_type    = var.release_type
    spring_profile  = var.spring_profile
    aws_region      = var.aws_region
  }))

  tag_specifications {
    resource_type = "instance"

    tags = merge(var.tags, {
      Name = var.app_name
    })
  }

  tag_specifications {
    resource_type = "volume"

    tags = merge(var.tags, {
      Name = "${var.app_name}-volume"
    })
  }

  tags = merge(var.tags, {
    Name = var.launch_template_name
  })
}
