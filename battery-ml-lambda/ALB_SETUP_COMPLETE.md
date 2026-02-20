# ✅ Application Load Balancer Setup Complete

## Summary

Your ECS service is now running behind an Application Load Balancer (ALB) with a **permanent DNS name** that never changes.

## ALB Details

- **DNS Name**: `http://battery-ml-alb-1652817744.us-east-1.elb.amazonaws.com`
- **Target Group**: battery-ml-tg (port 8000)
- **Health Check**: Passing ✅
- **ECS Service**: battery-ml-service-alb (ACTIVE)
- **Status**: HEALTHY

## Benefits

✅ **Permanent URL** - DNS name never changes even when ECS tasks restart  
✅ **High Availability** - Can scale to multiple tasks behind ALB  
✅ **Health Checks** - ALB automatically monitors backend health  
✅ **Auto Recovery** - Unhealthy tasks are automatically replaced  
✅ **Zero Downtime Deployments** - Rolling updates supported

## Configuration

### Vercel Environment Variable (REQUIRED)

Update your Vercel environment variable to use the ALB DNS name:

1. Go to: https://vercel.com/dashboard
2. Select **Zeflash2.0** project
3. Go to **Settings** → **Environment Variables**
4. Update or create:
   - **Name**: `VITE_ML_BACKEND_URL`
   - **Value**: `http://battery-ml-alb-1652817744.us-east-1.elb.amazonaws.com`
   - **Environments**: Production ✓, Preview ✓, Development ✓
5. **Save** and **Redeploy** your application

### Local Development

No changes needed! The frontend auto-detects dev mode and uses `http://localhost:8000` directly.

## Architecture

```
┌─────────────────┐
│  Vercel Frontend│
│  (Production)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────────┐
│  /api/ml-proxy  │─────▶│  Application     │
│  (Serverless)   │      │  Load Balancer   │
└─────────────────┘      └────────┬─────────┘
                                  │
                         ┌────────┴─────────┐
                         ▼                  ▼
                 ┌───────────────┐  ┌───────────────┐
                 │  ECS Task 1   │  │  ECS Task N   │
                 │  Port 8000    │  │  Port 8000    │
                 └───────────────┘  └───────────────┘
```

## Services Created/Updated

1. **ALB**: battery-ml-alb (already existed)
2. **Target Group**: battery-ml-tg (already existed)
3. **Security Group**: battery-ml-alb-sg (already existed)
4. **ECS Service**: battery-ml-service-alb (NEW - replaces battery-ml-service-test)

## Old vs New

| Component | Old (Direct IP) | New (ALB) |
|-----------|----------------|-----------|
| URL | `http://13.219.229.219:8000` | `http://battery-ml-alb-1652817744.us-east-1.elb.amazonaws.com` |
| Stability | ❌ Changes on restart | ✅ Never changes |
| Scalability | ❌ Single instance | ✅ Multiple tasks |
| Maintenance | ❌ Manual IP updates | ✅ Zero maintenance |
| Cost | Free | ~$16-20/month |

## Testing

Test the ALB endpoint:

```bash
# Health check
curl http://battery-ml-alb-1652817744.us-east-1.elb.amazonaws.com/health

# Inference endpoint (requires POST with data)
curl -X POST http://battery-ml-alb-1652817744.us-east-1.elb.amazonaws.com/api/v1/inference/trigger \
  -H "Content-Type: application/json" \
  -d '{"evse_id":"032300130C03064","connector_id":14,"limit":60}'
```

## Monitoring

Check ALB status:
```bash
# Target health
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:us-east-1:070872471952:targetgroup/battery-ml-tg/b666c9df3efc0bd3

# ECS service status
aws ecs describe-services --cluster ml-cluster --services battery-ml-service-alb
```

## Old Service Cleanup (Optional)

The old service `battery-ml-service-test` is still running. You can delete it:

```bash
# Scale down old service
aws ecs update-service --cluster ml-cluster --service battery-ml-service-test --desired-count 0

# Delete old service
aws ecs delete-service --cluster ml-cluster --service battery-ml-service-test --force
```

## Next Steps

1. ✅ Update Vercel environment variable with ALB DNS name
2. ✅ Redeploy Vercel application
3. ✅ Test AI Report button on deployed site
4. ✅ (Optional) Delete old ECS service

## Cost

- **ALB**: ~$16-20/month
  - $0.0225/hour (~$16.20/month)
  - $0.008 per LCU-hour (minimal with low traffic)

## Support

If you need to update the backend:

```bash
# Build and push new Docker image
cd battery-ml-lambda
docker build -t 070872471952.dkr.ecr.us-east-1.amazonaws.com/battery-ml:test .
docker push 070872471952.dkr.ecr.us-east-1.amazonaws.com/battery-ml:test

# Force new deployment (ALB handles rolling update)
aws ecs update-service --cluster ml-cluster --service battery-ml-service-alb --force-new-deployment
```

The ALB will handle zero-downtime rolling updates automatically! 🚀
