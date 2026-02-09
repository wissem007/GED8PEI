import { useState, useEffect, useCallback } from 'react';
import {
  Box, Paper, Typography, Button, Chip, IconButton, Tooltip,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  FormControl, InputLabel, Select, MenuItem, Grid, Card, CardContent,
  Dialog, DialogTitle, DialogContent, DialogActions, Alert as MuiAlert,
  CircularProgress, Snackbar, Badge
} from '@mui/material';
import {
  Refresh as RefreshIcon, Check as CheckIcon, Done as DoneIcon,
  Block as BlockIcon, AutoFixHigh as GenerateIcon, Delete as DeleteIcon,
  Warning as WarningIcon, Error as ErrorIcon, Info as InfoIcon,
  CheckCircle as CheckCircleIcon
} from '@mui/icons-material';
import { alertsAPI } from '../services/api';

// Icones par severite
const severityIcons = {
  CRITICAL: <ErrorIcon sx={{ color: '#d32f2f' }} />,
  HIGH: <ErrorIcon sx={{ color: '#f44336' }} />,
  MEDIUM: <WarningIcon sx={{ color: '#ff9800' }} />,
  LOW: <CheckCircleIcon sx={{ color: '#4caf50' }} />,
  INFO: <InfoIcon sx={{ color: '#2196f3' }} />,
};

function Alerts() {
  const [alerts, setAlerts] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    status: 'ACTIVE',
    severity: '',
    alertType: '',
  });
  const [severities, setSeverities] = useState([]);
  const [alertTypes, setAlertTypes] = useState([]);
  const [statuses, setStatuses] = useState([]);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [generateDialog, setGenerateDialog] = useState(false);
  const [generating, setGenerating] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [alertsRes, statsRes] = await Promise.all([
        alertsAPI.filter(filters),
        alertsAPI.getStats(),
      ]);
      setAlerts(alertsRes.data);
      setStats(statsRes.data);
    } catch (error) {
      console.error('Erreur chargement alertes:', error);
      setSnackbar({ open: true, message: 'Erreur lors du chargement des alertes', severity: 'error' });
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const loadFilters = async () => {
    try {
      const [sevRes, typesRes, statusesRes] = await Promise.all([
        alertsAPI.getSeverities(),
        alertsAPI.getTypes(),
        alertsAPI.getStatuses(),
      ]);
      setSeverities(sevRes.data);
      setAlertTypes(typesRes.data);
      setStatuses(statusesRes.data);
    } catch (error) {
      console.error('Erreur chargement filtres:', error);
    }
  };

  useEffect(() => {
    loadFilters();
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleFilterChange = (field, value) => {
    setFilters(prev => ({ ...prev, [field]: value }));
  };

  const handleGenerateAlerts = async () => {
    setGenerating(true);
    try {
      const response = await alertsAPI.generateAlerts();
      setSnackbar({
        open: true,
        message: `${response.data.created} alertes creees, ${response.data.skipped} ignorees`,
        severity: 'success'
      });
      setGenerateDialog(false);
      loadData();
    } catch (error) {
      console.error('Erreur generation alertes:', error);
      setSnackbar({ open: true, message: 'Erreur lors de la generation des alertes', severity: 'error' });
    } finally {
      setGenerating(false);
    }
  };

  const handleAcknowledge = async (id) => {
    try {
      await alertsAPI.acknowledge(id, 'user');
      setSnackbar({ open: true, message: 'Alerte prise en compte', severity: 'success' });
      loadData();
    } catch (error) {
      console.error('Erreur acknowledge:', error);
      setSnackbar({ open: true, message: 'Erreur', severity: 'error' });
    }
  };

  const handleResolve = async (id) => {
    try {
      await alertsAPI.resolve(id, 'user');
      setSnackbar({ open: true, message: 'Alerte resolue', severity: 'success' });
      loadData();
    } catch (error) {
      console.error('Erreur resolve:', error);
      setSnackbar({ open: true, message: 'Erreur', severity: 'error' });
    }
  };

  const handleIgnore = async (id) => {
    try {
      await alertsAPI.ignore(id);
      setSnackbar({ open: true, message: 'Alerte ignoree', severity: 'info' });
      loadData();
    } catch (error) {
      console.error('Erreur ignore:', error);
      setSnackbar({ open: true, message: 'Erreur', severity: 'error' });
    }
  };

  const getSeverityChip = (severity, severityDisplay, severityColor) => (
    <Chip
      icon={severityIcons[severity]}
      label={severityDisplay}
      size="small"
      sx={{
        backgroundColor: severityColor + '20',
        color: severityColor,
        fontWeight: 'bold',
        '& .MuiChip-icon': { color: severityColor }
      }}
    />
  );

  const getStatusChip = (status, statusDisplay) => {
    const colors = {
      ACTIVE: { bg: '#ffebee', color: '#d32f2f' },
      ACKNOWLEDGED: { bg: '#fff3e0', color: '#ff9800' },
      RESOLVED: { bg: '#e8f5e9', color: '#4caf50' },
      IGNORED: { bg: '#eceff1', color: '#607d8b' },
    };
    const style = colors[status] || colors.ACTIVE;
    return (
      <Chip
        label={statusDisplay}
        size="small"
        sx={{ backgroundColor: style.bg, color: style.color, fontWeight: 'bold' }}
      />
    );
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Alertes de Conformite</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="contained"
            color="primary"
            startIcon={<GenerateIcon />}
            onClick={() => setGenerateDialog(true)}
          >
            Analyser
          </Button>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadData}
          >
            Actualiser
          </Button>
        </Box>
      </Box>

      {/* Statistiques */}
      {stats && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ bgcolor: '#ffebee' }}>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>Alertes Actives</Typography>
                <Typography variant="h3" sx={{ color: '#d32f2f' }}>
                  {stats.activeCount || 0}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ bgcolor: '#fff3e0' }}>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>Prises en compte</Typography>
                <Typography variant="h3" sx={{ color: '#ff9800' }}>
                  {stats.acknowledgedCount || 0}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ bgcolor: '#e8f5e9' }}>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>Resolues</Typography>
                <Typography variant="h3" sx={{ color: '#4caf50' }}>
                  {stats.resolvedCount || 0}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>Par Severite (Actives)</Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {stats.bySeverity && Object.entries(stats.bySeverity).map(([sev, count]) => (
                    count > 0 && (
                      <Chip
                        key={sev}
                        label={`${sev}: ${count}`}
                        size="small"
                        sx={{ fontSize: '0.75rem' }}
                      />
                    )
                  ))}
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Filtres */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={4} md={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Statut</InputLabel>
              <Select
                value={filters.status}
                label="Statut"
                onChange={(e) => handleFilterChange('status', e.target.value)}
              >
                <MenuItem value="">Tous</MenuItem>
                {statuses.map((s) => (
                  <MenuItem key={s.code} value={s.code}>{s.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4} md={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Severite</InputLabel>
              <Select
                value={filters.severity}
                label="Severite"
                onChange={(e) => handleFilterChange('severity', e.target.value)}
              >
                <MenuItem value="">Toutes</MenuItem>
                {severities.map((s) => (
                  <MenuItem key={s.code} value={s.code}>{s.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4} md={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Type</InputLabel>
              <Select
                value={filters.alertType}
                label="Type"
                onChange={(e) => handleFilterChange('alertType', e.target.value)}
              >
                <MenuItem value="">Tous</MenuItem>
                {alertTypes.map((t) => (
                  <MenuItem key={t.code} value={t.code}>{t.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {/* Liste des alertes */}
      <TableContainer component={Paper}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : alerts.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}>
            <CheckCircleIcon sx={{ fontSize: 60, color: '#4caf50', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Aucune alerte {filters.status === 'ACTIVE' ? 'active' : ''}
            </Typography>
            <Typography color="text.secondary">
              Cliquez sur "Analyser" pour detecter les non-conformites
            </Typography>
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: '#f5f5f5' }}>
                <TableCell>Severite</TableCell>
                <TableCell>Titre</TableCell>
                <TableCell>Serveur</TableCell>
                <TableCell>Environnement</TableCell>
                <TableCell>Logiciel</TableCell>
                <TableCell>Version</TableCell>
                <TableCell>Statut</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {alerts.map((alert) => (
                <TableRow
                  key={alert.id}
                  sx={{
                    '&:hover': { bgcolor: '#fafafa' },
                    bgcolor: alert.severity === 'CRITICAL' ? '#fff5f5' : 'inherit'
                  }}
                >
                  <TableCell>
                    {getSeverityChip(alert.severity, alert.severityDisplay, alert.severityColor)}
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                      {alert.title}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      {alert.alertTypeDisplay}
                    </Typography>
                  </TableCell>
                  <TableCell>{alert.serverName}</TableCell>
                  <TableCell>
                    <Chip label={alert.environment} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell>{alert.softwareName}</TableCell>
                  <TableCell>
                    <Typography variant="body2">{alert.currentVersion}</Typography>
                    {alert.recommendedVersion && (
                      <Typography variant="caption" color="success.main">
                        → {alert.recommendedVersion}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {getStatusChip(alert.status, alert.statusDisplay)}
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption">
                      {new Date(alert.createdAt).toLocaleDateString('fr-FR')}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    {alert.status === 'ACTIVE' && (
                      <>
                        <Tooltip title="Prendre en compte">
                          <IconButton size="small" onClick={() => handleAcknowledge(alert.id)}>
                            <CheckIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Marquer resolue">
                          <IconButton size="small" color="success" onClick={() => handleResolve(alert.id)}>
                            <DoneIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Ignorer">
                          <IconButton size="small" onClick={() => handleIgnore(alert.id)}>
                            <BlockIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )}
                    {alert.status === 'ACKNOWLEDGED' && (
                      <Tooltip title="Marquer resolue">
                        <IconButton size="small" color="success" onClick={() => handleResolve(alert.id)}>
                          <DoneIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </TableContainer>

      {/* Dialog Generation */}
      <Dialog open={generateDialog} onClose={() => !generating && setGenerateDialog(false)}>
        <DialogTitle>Analyser les donnees d'obsolescence</DialogTitle>
        <DialogContent>
          <Typography>
            Cette action va analyser les donnees d'obsolescence PMT DISCOVR et generer
            des alertes pour les versions interdites, obsoletes, risquees, etc.
          </Typography>
          <MuiAlert severity="info" sx={{ mt: 2 }}>
            Les alertes existantes ne seront pas dupliquees.
          </MuiAlert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGenerateDialog(false)} disabled={generating}>
            Annuler
          </Button>
          <Button
            variant="contained"
            onClick={handleGenerateAlerts}
            disabled={generating}
            startIcon={generating ? <CircularProgress size={20} /> : <GenerateIcon />}
          >
            {generating ? 'Analyse en cours...' : 'Lancer l\'analyse'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <MuiAlert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </MuiAlert>
      </Snackbar>
    </Box>
  );
}

export default Alerts;
