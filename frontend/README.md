# QueryLens Frontend

Modern React-based user interface for the QueryLens SQL query analysis tool.

## Features

- 🎨 Beautiful, gradient-based UI design
- 📝 SQL query input with syntax highlighting
- 📊 Real-time performance metrics visualization
- 💡 Intelligent optimization suggestions display
- ✨ Optimized query comparison and copy functionality
- 📱 Responsive design for all screen sizes

## Development

### Install Dependencies

```bash
npm install
```

### Start Development Server

```bash
npm start
```

The app will open at [http://localhost:3000](http://localhost:3000).

### Build for Production

```bash
npm run build
```

Builds the app for production to the `build/` folder.

### Run Tests

```bash
npm test
```

## Environment Variables

Create a `.env` file in the frontend directory:

```env
REACT_APP_API_URL=http://localhost:8080
```

## Project Structure

```
frontend/
├── public/          # Static assets
├── src/
│   ├── components/  # React components
│   │   ├── Header.js
│   │   ├── QueryInput.js
│   │   ├── Results.js
│   │   ├── MetricsCard.js
│   │   ├── SuggestionsCard.js
│   │   └── OptimizedQueryCard.js
│   ├── App.js       # Main application component
│   ├── App.css      # Global styles
│   └── index.js     # Entry point
├── Dockerfile       # Container configuration
└── package.json     # Dependencies and scripts
```

## Technologies

- React 18
- CSS3 (Custom styling)
- Axios (HTTP client)
- Nginx (Production server)

## Docker

Build the Docker image:

```bash
docker build -t querylens-frontend .
```

Run the container:

```bash
docker run -p 3000:3000 querylens-frontend
```

