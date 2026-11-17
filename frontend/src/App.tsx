import './App.css';
import { useAllVideos } from './useAllVideos';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { getSession, logout } from './auth';
import type { RegisteredUser } from './auth';
import RegistrationForm from './components/RegistrationForm';
import LoginForm from './components/LoginForm';
import VideoGrid from './components/VideoGrid';
import VideoDetailPage from './components/VideoDetailPage';
import ProfilePage from './components/ProfilePage';
import UploadVideoPage from './components/UploadVideoPage';
import AnalyticsPage from './components/AnalyticsPage';

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

  useEffect(() => {
    if ((path === '/upload' || path === '/analytics') && !sessionLoading && !user) {
      window.history.replaceState({}, '', '/login');
      setPath('/login');
    }
  }, [path, sessionLoading, user]);

  const detailMatch = path.match(/^\/videos\/([^/]+)$/);
  const detailId = detailMatch ? Number(detailMatch[1]) : null;
  const detailPath = detailMatch ? detailMatch[1] : null;
  const isDetailRoute = detailPath !== null;
  const validDetailId = detailId !== null && Number.isInteger(detailId) && detailId > 0 ? detailId : null;
  const profileMatch = path.match(/^\/users\/([1-9]\d*)$/);
  const publicProfileId = profileMatch ? Number(profileMatch[1]) : null;
  const profileRoute = path === '/profile' || path.startsWith('/users/');

  return (
    <div className="App">
      <header className="App-header">
        <a
          className="brand"
          href="/"
          aria-label="ProTube home"
          onClick={(event) => {
            event.preventDefault();
            navigate('/');
          }}
        >
          <span className="brand-icon" aria-hidden="true">
            &#9654;
          </span>
          <span>ProTube</span>
        </a>
        <nav className="user-navigation" aria-label="User navigation">
          {user ? (
            <>
              <p role="status">Signed in as {user.email}</p>
              <a
                href="/upload"
                onClick={(event) => {
                  event.preventDefault();
                  navigate('/upload');
                }}
              >
                Upload Video
              </a>
              <a
                href="/profile"
                onClick={(event) => {
                  event.preventDefault();
                  navigate('/profile');
                }}
              >
                My profile
              </a>
              <a
                href="/analytics"
                onClick={(event) => {
                  event.preventDefault();
                  navigate('/analytics');
                }}
              >
                My analytics
              </a>
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
        </nav>
      </header>
      <main className="App-content">
        {sessionLoading ? (
          <p role="status">Checking session...</p>
        ) : path === '/analytics' ? (
          user ? (
            <AnalyticsPage onOpenVideo={(id) => navigate(`/videos/${id}`)} onBack={() => navigate('/')} />
          ) : (
            <p role="status">Redirecting to login...</p>
          )
        ) : path === '/upload' ? (
          user ? (
            <UploadVideoPage
              onUploaded={(id) => navigate(`/videos/${id}`)}
              onLogin={() => {
                setUser(null);
                navigate('/login');
              }}
              onBack={() => navigate('/')}
            />
          ) : (
            <p role="status">Redirecting to login…</p>
          )
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
        ) : profileRoute &&
          (path === '/profile' || (publicProfileId !== null && Number.isSafeInteger(publicProfileId))) ? (
          <ProfilePage
            key={`${path}:${user?.id ?? 'visitor'}`}
            profileId={publicProfileId}
            user={user}
            onLogin={() => navigate('/login')}
            onBack={() => navigate('/')}
          />
        ) : profileRoute ? (
          <section role="alert">
            <h1>Profile not found</h1>
          </section>
        ) : isDetailRoute && validDetailId ? (
          <VideoDetailPage
            key={`${validDetailId}:${user?.id ?? 'visitor'}`}
            videoId={validDetailId}
            onBack={() => navigate('/')}
            user={user}
            onLoginRequest={() => navigate('/login')}
          />
        ) : isDetailRoute ? (
          <section role="alert" aria-labelledby="video-not-found-title">
            <h1 id="video-not-found-title">Video not found</h1>
            <p>The video you requested is not available.</p>
            <button type="button" onClick={() => navigate('/')}>
              Back to catalog
            </button>
          </section>
        ) : (
          <ContentApp onOpenVideo={(videoId) => navigate(`/videos/${videoId}`)} />
        )}
      </main>
    </div>
  );
}

function ContentApp({ onOpenVideo }: { onOpenVideo: (videoId: number) => void }) {
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const { loading, message, value, retry } = useAllVideos(page, submittedQuery);
  const pending = loading === 'loading' || loading === 'idle';

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setSubmittedQuery(query.trim());
  }

  return (
    <section className="catalog" aria-labelledby="catalog-title">
      <div className="catalog-heading">
        <p className="eyebrow">Explore ProTube</p>
        <h1 id="catalog-title">Available videos</h1>
        <p>Find your next favorite video.</p>
      </div>
      <form className="video-search" aria-label="Search videos" onSubmit={search}>
        <label htmlFor="video-search-query">Search videos</label>
        <input
          id="video-search-query"
          type="search"
          placeholder="Search videos..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <button type="submit" disabled={pending}>
          Search
        </button>
      </form>
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
        <VideoGrid videos={value.content} onOpenVideo={onOpenVideo} />
      ) : (
        <p>{submittedQuery ? `No videos found for "${submittedQuery}".` : 'No videos available.'}</p>
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
