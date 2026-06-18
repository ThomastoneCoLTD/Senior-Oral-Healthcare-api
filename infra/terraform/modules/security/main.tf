resource "aws_security_group" "alb" {
  name        = "${var.name_prefix}-alb-sg"
  description = "SOH API ALB security group"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-alb-sg"
  })
}

resource "aws_security_group_rule" "alb_https_ipv4" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  protocol          = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_blocks       = var.alb_allowed_cidrs
}

resource "aws_security_group_rule" "alb_https_ipv6" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  protocol          = "tcp"
  from_port         = 443
  to_port           = 443
  ipv6_cidr_blocks  = var.alb_allowed_ipv6_cidrs
}

resource "aws_security_group_rule" "alb_egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.alb.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
  ipv6_cidr_blocks  = ["::/0"]
}

resource "aws_security_group" "ec2" {
  name        = "${var.name_prefix}-ec2-sg"
  description = "SOH API EC2 security group"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-ec2-sg"
  })
}

resource "aws_security_group_rule" "ec2_app_from_alb" {
  type                     = "ingress"
  security_group_id        = aws_security_group.ec2.id
  protocol                 = "tcp"
  from_port                = var.app_port
  to_port                  = var.app_port
  source_security_group_id = aws_security_group.alb.id
}

resource "aws_security_group_rule" "ec2_egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.ec2.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
  ipv6_cidr_blocks  = ["::/0"]
}
