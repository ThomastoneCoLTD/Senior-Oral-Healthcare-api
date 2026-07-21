resource "aws_iam_role" "ec2" {
  name = "${var.name_prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy" "artifact_read" {
  name = "${var.name_prefix}-artifact-read"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadSohApiArtifacts"
        Effect = "Allow"
        Action = ["s3:GetObject"]
        Resource = [
          "arn:aws:s3:::${var.artifact_bucket}/${var.artifact_prefix}/${var.release_type}/app.jar",
          "arn:aws:s3:::${var.artifact_bucket}/${var.artifact_prefix}/${var.release_type}/.env",
          "arn:aws:s3:::${var.artifact_bucket}/${var.artifact_prefix}/video/*",
          "arn:aws:s3:::${var.artifact_bucket}/${var.artifact_prefix}/video-thumbnails/*"
        ]
      },
      {
        Sid    = "WriteSohApiRuntimeUploads"
        Effect = "Allow"
        Action = ["s3:PutObject"]
        Resource = [
          "arn:aws:s3:::${var.artifact_bucket}/${var.artifact_prefix}/${var.release_type}/*"
        ]
      },
      {
        Sid    = "DenyRuntimeOverwriteOfDeployArtifacts"
        Effect = "Deny"
        Action = ["s3:PutObject"]
        Resource = [
          "arn:aws:s3:::${var.artifact_bucket}/${var.artifact_prefix}/${var.release_type}/app.jar",
          "arn:aws:s3:::${var.artifact_bucket}/${var.artifact_prefix}/${var.release_type}/.env"
        ]
      },
      {
        Sid      = "SynthesizeTtsSpeech"
        Effect   = "Allow"
        Action   = ["polly:SynthesizeSpeech"]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  count = var.enable_ssm ? 1 : 0

  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.name_prefix}-ec2-instance-profile"
  role = aws_iam_role.ec2.name

  tags = var.tags
}
