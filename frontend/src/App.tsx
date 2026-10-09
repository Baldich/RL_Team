import './App.css';
import { useAllVideos } from './useAllVideos';
import { useEffect, useState } from 'react';
import { getSession } from './auth';
import type { RegisteredUser } from './auth';
import RegistrationForm from './components/RegistrationForm';

function App() {
  const [user, setUser] = useState<RegisteredUser | null>(null);
  const [path, setPath] = useState(window.location.pathname);
  const [sessionLoading, setSessionLoading] = useState(true);

  useEffect(() => {
    let active = true;
    getSession()
      .then((session) => {
        if (active) setUser(session);
      })
      .catch(() => {
        // A failed session lookup leaves registration available to visitors.
      })
      .finally(() => {
        if (active) setSessionLoading(false);
      });
    const onPopState = () => setPath(window.location.pathname);
    window.addEventListener('popstate', onPopState);
    return () => {
      active = false;
      window.removeEventListener('popstate', onPopState);
    };
  }, []);

  function navigate(nextPath: string) {
    window.history.pushState({}, '', nextPath);
    setPath(nextPath);
  }

  return (
    <div className="App">
      <header className="App-header">
        <img src="/protube-logo-removebg-preview.png" className="App-logo" alt="logo" />
        {user ? (
          <p role="status">Signed in as {user.email}</p>
        ) : (
          <a
            href="/register"
            onClick={(event) => {
              event.preventDefault();
              navigate('/register');
            }}
          >
            Register
          </a>
        )}
        {sessionLoading ? (
          <p role="status">Checking session...</p>
        ) : path === '/register' ? (
          <RegistrationForm
            onRegistered={(registered) => {
              setUser(registered);
              navigate('/');
            }}
          />
        ) : (
          <ContentApp />
        )}
      </header>
    </div>
  );
}

function ContentApp() {
  const { loading, message, value } = useAllVideos();
  switch (loading) {
    case 'loading':
      return <div>Loading...</div>;
    case 'error':
      return (
        <div>
          <h3>Error</h3> <p>{message}</p>
        </div>
      );
    case 'success':
      return (
        <>
          <strong>Videos availables:</strong>
          <ul>
            {value.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </>
      );
  }
  return <div>Idle...</div>;
}

export default App;
