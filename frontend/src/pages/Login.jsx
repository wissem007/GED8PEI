import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Alert,
  InputAdornment, IconButton, CircularProgress
} from '@mui/material';
import {
  Visibility, VisibilityOff, Login as LoginIcon
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';

function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const from = location.state?.from?.pathname || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!username || !password) {
      setError('Veuillez remplir tous les champs');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await login(username, password);
      navigate(from, { replace: true });
    } catch (err) {
      console.error('Erreur connexion:', err);
      setError(err.response?.data?.message || 'Identifiants invalides');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight="100vh"
      bgcolor="background.default"
    >
      <Paper
        elevation={3}
        sx={{
          p: 4,
          width: '100%',
          maxWidth: 400,
          mx: 2
        }}
      >
        {/* Logo */}
        <Box display="flex" flexDirection="column" alignItems="center" mb={4}>
          <Box
            component="img"
            src="/logo-edf.svg"
            alt="EDF"
            sx={{ height: 48, width: 'auto', mb: 2 }}
          />
          <Typography variant="h5" fontWeight="bold">
            GED-PEI
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Gestion des serveurs
          </Typography>
        </Box>

        {/* Formulaire */}
        <form onSubmit={handleSubmit}>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <TextField
            fullWidth
            label="Nom d'utilisateur"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            margin="normal"
            autoComplete="username"
            autoFocus
            disabled={loading}
          />

          <TextField
            fullWidth
            label="Mot de passe"
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            margin="normal"
            autoComplete="current-password"
            disabled={loading}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowPassword(!showPassword)}
                    edge="end"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              )
            }}
          />

          <Button
            type="submit"
            fullWidth
            variant="contained"
            size="large"
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} /> : <LoginIcon />}
            sx={{ mt: 3, mb: 2 }}
          >
            {loading ? 'Connexion...' : 'Se connecter'}
          </Button>
        </form>

        {/* Info */}
        <Box mt={3} p={2} bgcolor="grey.100" borderRadius={1}>
          <Typography variant="body2" color="text.secondary" align="center">
            Comptes par défaut :
          </Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            admin / admin123
          </Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            user / user123
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
}

export default Login;
