import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

const rootElement = document.getElementById('root');

if (rootElement) {
  // NOTE: StrictMode is intentionally omitted because it double-invokes useEffect
  // in dev builds, which causes Keycloak init to run twice and creates redirect loops.
  ReactDOM.createRoot(rootElement).render(<App />);
}
