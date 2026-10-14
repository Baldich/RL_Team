import './App.css';
import { useAllVideos } from './useAllVideos';
import { useEffect, useState } from 'react';
import { getSession, logout } from './auth';
import type { RegisteredUser } from './auth';
import RegistrationForm from './components/RegistrationForm';
import LoginForm from './components/LoginForm';
import VideoGrid from './components/VideoGrid';

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
          <>
            <p role="status">Signed in as {user.email}</p>
            <button type="button" onClick={() => logout().then(() => setUser(null))}>
              Log out
            </button>
          </>
        ) : (
          <>
            <a
              href="/login"
              onClick={(event) => {
                event.preventDefault();
                navigate('/login');
              }}
            >
              Log in
            </a>
            <a
              href="/register"
              onClick={(event) => {
                event.preventDefault();
                navigate('/register');
              }}
            >
              Register
            </a>
          </>
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
        ) : path === '/login' ? (
          <LoginForm
            onLoggedIn={(loggedIn) => {
              setUser(loggedIn);
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
  const [page, setPage] = useState(0);
  const { loading, message, value, retry } = useAllVideos(page);
  const pending = loading === 'loading' || loading === 'idle';
  return (
    <section className="catalog" aria-labelledby="catalog-title">
      <h1 id="catalog-title">Available videos</h1>
      {pending ? (
        <p role="status">Loading...</p>
      ) : loading === 'error' ? (
        <div role="alert">
          <h2>Error</h2>
          <p>{message}</p>
          <button type="button" onClick={retry}>
            Retry
          </button>
        </div>
      ) : value && value.content.length > 0 ? (
        <VideoGrid videos={value.content} />
      ) : (
        <p>No videos available.</p>
      )}
      <nav aria-label="Video pagination">
        <button type="button" disabled={pending || page === 0} onClick={() => setPage((current) => current - 1)}>
          Previous
        </button>
        <span>
          Page {page + 1} of {Math.max(1, value?.totalPages ?? 0)}
        </span>
        <button
          type="button"
          disabled={pending || loading === 'error' || !value || page + 1 >= value.totalPages}
          onClick={() => setPage((current) => current + 1)}
        >
          Next
        </button>
      </nav>
    </section>
  );
}

export default App;
