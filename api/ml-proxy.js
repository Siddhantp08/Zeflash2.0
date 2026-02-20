// ML Backend Proxy - Vercel Serverless Function
// Routes requests to AWS ECS backend with proper CORS handling

export default async function handler(req, res) {
  // Get backend URL from environment variable
  // This should be set in Vercel: VITE_ML_BACKEND_URL=http://13.219.229.219:8000
  const ML_BACKEND_URL = process.env.VITE_ML_BACKEND_URL || 'http://localhost:8000';
  
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, HEAD');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  // Handle preflight
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  try {
    // Get the full path from query parameter (e.g., /api/v1/inference/trigger)
    const path = req.url.replace('/api/ml-proxy', '') || '/';
    const targetUrl = `${ML_BACKEND_URL}${path}`;
    
    console.log('🔄 ML Proxy:', req.method, targetUrl);

    const options = {
      method: req.method || 'GET',
      headers: { 
        'Content-Type': 'application/json',
        'Accept': 'application/json, image/png, */*'
      }
    };
    
    if ((req.method === 'POST' || req.method === 'PUT') && req.body) {
      options.body = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
    }
    
    const response = await fetch(targetUrl, options);
    
    // Handle image responses
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('image')) {
      const buffer = await response.arrayBuffer();
      res.setHeader('Content-Type', contentType);
      return res.status(response.status).send(Buffer.from(buffer));
    }
    
    // Handle JSON responses
    const data = await response.json();
    console.log(`✅ Response status: ${response.status}`);
    return res.status(response.status).json(data);
    
  } catch (error) {
    console.error('❌ ML Proxy error:', error);
    return res.status(500).json({ 
      error: 'Cannot reach ML backend server',
      message: error.message,
      backend: process.env.VITE_ML_BACKEND_URL || 'Not configured'
    });
  }
}
