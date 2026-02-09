import { useState, useEffect, useCallback } from 'react';
import {
  Box, Paper, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, TextField, FormControl, InputLabel, Select, MenuItem,
  Chip, Button, Alert, CircularProgress, Card, CardContent, Grid,
  Accordion, AccordionSummary, AccordionDetails, Dialog, DialogTitle,
  DialogContent, DialogActions, Tooltip, IconButton, Tabs, Tab
} from '@mui/material';
import {
  Upload as UploadIcon, Refresh as RefreshIcon, Search as SearchIcon,
  ExpandMore as ExpandMoreIcon, Delete as DeleteIcon,
  CheckCircle as CheckIcon, Warning as WarningIcon, Error as ErrorIcon,
  Info as InfoIcon, Storage as StorageIcon
} from '@mui/icons-material';
import { serverObsolescenceAPI } from '../services/api';

const statusColors = {
  PRECONISEE: { bg: '#e8f5e9', color: '#2e7d32', icon: CheckIcon },
  TRAJECTOIRE_PRECONISEE: { bg: '#e3f2fd', color: '#1565c0', icon: InfoIcon },
  TOLEREE: { bg: '#fff3e0', color: '#ef6c00', icon: WarningIcon },
  RISQUEE: { bg: '#fff8e1', color: '#f57c00', icon: WarningIcon },
  INTERDITE: { bg: '#ffebee', color: '#c62828', icon: ErrorIcon },
  INTERDITE_CYBER: { bg: '#fce4ec', color: '#ad1457', icon: ErrorIcon },
  OBSOLETE: { bg: '#f5f5f5', color: '#616161', icon: ErrorIcon },
};

function ServerObsolescence() {
  const [data, setData] = useState([]);
  const [groupedData, setGroupedData] = useState({});
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ environment: '', serverName: '', status: '', softwareName: '' });
  const [environments, setEnvironments] = useState([]);
  const [servers, setServers] = useState([]);
  const [statuses, setStatuses] = useState([]);
  const [stats, setStats] = useState({});
  const [importDialog, setImportDialog] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);
  const [viewMode, setViewMode] = useState(0); // 0 = TCD, 1 = Liste
  const [expandedEnvs, setExpandedEnvs] = useState({});
  const [expandedServers, setExpandedServers] = useState({});

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [dataRes, groupedRes] = await Promise.all([
        serverObsolescenceAPI.filter(filters),
        serverObsolescenceAPI.getGrouped(),
      ]);
      setData(dataRes.data || []);
      setGroupedData(groupedRes.data?.data || {});
    } catch (err) {
      console.error('Erreur chargement donnees:', err);
      setError('Erreur lors du chargement des donnees');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const fetchMetadata = useCallback(async () => {
    try {
      const [envsRes, serversRes, statusesRes, statsRes] = await Promise.all([
        serverObsolescenceAPI.getEnvironments(),
        serverObsolescenceAPI.getServers(),
        serverObsolescenceAPI.getStatuses(),
        serverObsolescenceAPI.getStats(),
      ]);
      setEnvironments(envsRes.data || []);
      setServers(serversRes.data || []);
      setStatuses(statusesRes.data || []);
      setStats(statsRes.data || {});
    } catch (err) {
      console.error('Erreur chargement metadata:', err);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    fetchMetadata();
  }, [fetchMetadata]);

  const handleFilterChange = (field, value) => {
    setFilters(prev => ({ ...prev, [field]: value }));
  };

  const handleImport = async () => {
    if (!importFile) return;

    setImporting(true);
    setImportResult(null);
    try {
      const response = await serverObsolescenceAPI.importCsv(importFile);
      setImportResult(response.data);
      fetchData();
      fetchMetadata();
    } catch (err) {
      console.error('Erreur import:', err);
      setImportResult({ error: err.response?.data?.message || 'Erreur lors de l\'import' });
    } finally {
      setImporting(false);
    }
  };

  const handleClearAll = async () => {
    if (!window.confirm('Supprimer toutes les donnees d\'obsolescence ?')) return;
    try {
      await serverObsolescenceAPI.clearAll();
      fetchData();
      fetchMetadata();
    } catch (err) {
      setError('Erreur lors de la suppression');
    }
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      setImportFile(file);
      setImportResult(null);
    }
  };

  const getStatusChip = (status) => {
    const config = statusColors[status] || { bg: '#f5f5f5', color: '#757575' };
    return (
      <Chip
        label={status?.replace('_', ' ')}
        size="small"
        sx={{
          bgcolor: config.bg,
          color: config.color,
          fontWeight: 'medium',
          fontSize: '0.7rem',
        }}
      />
    );
  };

  const getStatusIcon = (status) => {
    const config = statusColors[status];
    if (!config) return <InfoIcon sx={{ color: '#757575' }} />;
    const IconComponent = config.icon;
    return <IconComponent sx={{ color: config.color, fontSize: 20 }} />;
  };

  const toggleEnv = (env) => {
    setExpandedEnvs(prev => ({ ...prev, [env]: !prev[env] }));
  };

  const toggleServer = (server) => {
    setExpandedServers(prev => ({ ...prev, [server]: !prev[server] }));
  };

  const renderTCDView = () => {
    if (Object.keys(groupedData).length === 0) {
      return (
        <Alert severity="info">
          Aucune donnee d'obsolescence. Importez un fichier CSV PMT DISCOVR.
        </Alert>
      );
    }

    return (
      <Box>
        {Object.entries(groupedData).map(([env, serverMap]) => (
          <Accordion
            key={env}
            expanded={expandedEnvs[env] !== false}
            onChange={() => toggleEnv(env)}
            sx={{ mb: 1 }}
          >
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Box display="flex" alignItems="center" gap={2}>
                <Chip
                  label={env}
                  color={env === 'PROD' ? 'error' : env === 'PREPROD' ? 'warning' : 'primary'}
                  size="small"
                />
                <Typography variant="subtitle1" fontWeight="bold">
                  {Object.keys(serverMap).length} serveur(s)
                </Typography>
              </Box>
            </AccordionSummary>
            <AccordionDetails>
              {Object.entries(serverMap).map(([serverName, statusMap]) => (
                <Accordion
                  key={serverName}
                  expanded={expandedServers[serverName] !== false}
                  onChange={() => toggleServer(serverName)}
                  sx={{ ml: 2, mb: 1 }}
                >
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Box display="flex" alignItems="center" gap={2} width="100%">
                      <StorageIcon color="action" />
                      <Typography fontWeight="medium">{serverName}</Typography>
                      <Box display="flex" gap={0.5} ml="auto" mr={2}>
                        {Object.entries(statusMap).map(([status, items]) => (
                          <Tooltip key={status} title={`${status}: ${items.length}`}>
                            <Chip
                              label={items.length}
                              size="small"
                              sx={{
                                bgcolor: statusColors[status]?.bg || '#f5f5f5',
                                color: statusColors[status]?.color || '#757575',
                                minWidth: 30,
                              }}
                            />
                          </Tooltip>
                        ))}
                      </Box>
                    </Box>
                  </AccordionSummary>
                  <AccordionDetails>
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow sx={{ bgcolor: 'grey.100' }}>
                            <TableCell width={150}><strong>Statut</strong></TableCell>
                            <TableCell><strong>Logiciel</strong></TableCell>
                            <TableCell><strong>Version</strong></TableCell>
                            <TableCell><strong>Fin Support</strong></TableCell>
                            <TableCell><strong>OS Cible</strong></TableCell>
                            <TableCell><strong>Version Cible</strong></TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {Object.entries(statusMap).map(([status, items]) =>
                            items.map((item, idx) => (
                              <TableRow
                                key={`${status}-${idx}`}
                                sx={{ bgcolor: statusColors[status]?.bg || 'inherit' }}
                              >
                                {idx === 0 && (
                                  <TableCell
                                    rowSpan={items.length}
                                    sx={{
                                      bgcolor: statusColors[status]?.bg,
                                      borderRight: '2px solid',
                                      borderColor: statusColors[status]?.color,
                                    }}
                                  >
                                    <Box display="flex" alignItems="center" gap={1}>
                                      {getStatusIcon(status)}
                                      <Typography variant="caption" fontWeight="bold">
                                        {status?.replace('_', ' ')}
                                      </Typography>
                                    </Box>
                                  </TableCell>
                                )}
                                <TableCell>{item.softwareName}</TableCell>
                                <TableCell>
                                  <Chip label={item.currentVersion || '-'} size="small" variant="outlined" />
                                </TableCell>
                                <TableCell>{item.supportEndDate || '-'}</TableCell>
                                <TableCell>{item.osTarget || '-'}</TableCell>
                                <TableCell>
                                  {item.osActualTargetVersion && (
                                    <Chip
                                      label={item.osActualTargetVersion}
                                      size="small"
                                      color="primary"
                                      variant="outlined"
                                    />
                                  )}
                                </TableCell>
                              </TableRow>
                            ))
                          )}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </AccordionDetails>
                </Accordion>
              ))}
            </AccordionDetails>
          </Accordion>
        ))}
      </Box>
    );
  };

  const renderListView = () => (
    <TableContainer component={Paper}>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ bgcolor: 'grey.100' }}>
            <TableCell><strong>Environnement</strong></TableCell>
            <TableCell><strong>Serveur</strong></TableCell>
            <TableCell><strong>Statut</strong></TableCell>
            <TableCell><strong>Logiciel</strong></TableCell>
            <TableCell><strong>Version</strong></TableCell>
            <TableCell><strong>Fin Support</strong></TableCell>
            <TableCell><strong>OS Cible</strong></TableCell>
            <TableCell><strong>Version Cible</strong></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {loading ? (
            <TableRow>
              <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                <CircularProgress />
              </TableCell>
            </TableRow>
          ) : data.length === 0 ? (
            <TableRow>
              <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                <Typography color="text.secondary">
                  Aucune donnee trouvee
                </Typography>
              </TableCell>
            </TableRow>
          ) : (
            data.map((item) => (
              <TableRow key={item.id} hover sx={{ bgcolor: statusColors[item.status]?.bg }}>
                <TableCell>
                  <Chip
                    label={item.environment}
                    size="small"
                    color={item.environment === 'PROD' ? 'error' : item.environment === 'PREPROD' ? 'warning' : 'primary'}
                  />
                </TableCell>
                <TableCell><strong>{item.serverName}</strong></TableCell>
                <TableCell>{getStatusChip(item.status)}</TableCell>
                <TableCell>{item.softwareName}</TableCell>
                <TableCell>
                  <Chip label={item.currentVersion || '-'} size="small" variant="outlined" />
                </TableCell>
                <TableCell>{item.supportEndDate || '-'}</TableCell>
                <TableCell>{item.osTarget || '-'}</TableCell>
                <TableCell>{item.osActualTargetVersion || '-'}</TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight="bold">
          Obsolescence par Serveur
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => { fetchData(); fetchMetadata(); }}
            sx={{ mr: 1 }}
          >
            Actualiser
          </Button>
          <Button
            variant="outlined"
            color="error"
            startIcon={<DeleteIcon />}
            onClick={handleClearAll}
            sx={{ mr: 1 }}
          >
            Vider
          </Button>
          <Button
            variant="contained"
            startIcon={<UploadIcon />}
            onClick={() => setImportDialog(true)}
          >
            Importer CSV
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Statistiques */}
      {stats.byStatus && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {Object.entries(stats.byStatus).map(([label, count]) => (
            <Grid item xs={6} sm={4} md={2} key={label}>
              <Card variant="outlined">
                <CardContent sx={{ textAlign: 'center', py: 1 }}>
                  <Typography variant="h5" fontWeight="bold">{count}</Typography>
                  <Typography variant="caption" color="text.secondary">{label}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
          <Grid item xs={6} sm={4} md={2}>
            <Card variant="outlined" sx={{ bgcolor: 'primary.light' }}>
              <CardContent sx={{ textAlign: 'center', py: 1 }}>
                <Typography variant="h5" fontWeight="bold" color="white">{stats.serverCount || 0}</Typography>
                <Typography variant="caption" color="white">Serveurs</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Filtres */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Environnement</InputLabel>
              <Select
                value={filters.environment}
                label="Environnement"
                onChange={(e) => handleFilterChange('environment', e.target.value)}
              >
                <MenuItem value="">Tous</MenuItem>
                {environments.map((env) => (
                  <MenuItem key={env} value={env}>{env}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Serveur</InputLabel>
              <Select
                value={filters.serverName}
                label="Serveur"
                onChange={(e) => handleFilterChange('serverName', e.target.value)}
              >
                <MenuItem value="">Tous</MenuItem>
                {servers.map((s) => (
                  <MenuItem key={s} value={s}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={3}>
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
          <Grid item xs={12} sm={3}>
            <TextField
              fullWidth
              size="small"
              label="Logiciel"
              value={filters.softwareName}
              onChange={(e) => handleFilterChange('softwareName', e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ color: 'text.secondary', mr: 1 }} />,
              }}
            />
          </Grid>
        </Grid>
      </Paper>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={viewMode} onChange={(_, v) => setViewMode(v)}>
          <Tab label="Vue TCD (par serveur)" />
          <Tab label="Vue Liste" />
        </Tabs>
      </Box>

      {/* Contenu */}
      {loading ? (
        <Box display="flex" justifyContent="center" py={4}>
          <CircularProgress />
        </Box>
      ) : viewMode === 0 ? renderTCDView() : renderListView()}

      {/* Dialog Import */}
      <Dialog open={importDialog} onClose={() => setImportDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Importer les donnees d'obsolescence</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <Alert severity="info" sx={{ mb: 2 }}>
              <Typography variant="body2">
                <strong>Format attendu :</strong> Export CSV depuis PMT DISCOVR / Obsolescence Prevobs<br />
                Colonnes: ENVIRONNEMENT, INSTANCE, SERVEUR, CSR STATUT VERSION PACKAGE, INVENTIV NOM COMPOSANT, CSR VERSION PACKAGE, Date de Fin de Support, OS CIBLE...
              </Typography>
            </Alert>

            <Box
              sx={{
                border: '2px dashed',
                borderColor: 'grey.400',
                borderRadius: 2,
                p: 3,
                textAlign: 'center',
                bgcolor: 'grey.50',
                cursor: 'pointer',
                '&:hover': { borderColor: 'primary.main', bgcolor: 'grey.100' },
              }}
              component="label"
            >
              <input
                type="file"
                accept=".csv"
                onChange={handleFileChange}
                style={{ display: 'none' }}
              />
              <UploadIcon sx={{ fontSize: 48, color: 'grey.500', mb: 1 }} />
              <Typography>
                {importFile ? importFile.name : 'Cliquez pour selectionner un fichier CSV'}
              </Typography>
            </Box>

            {importResult && (
              <Alert
                severity={importResult.error ? 'error' : 'success'}
                sx={{ mt: 2 }}
              >
                {importResult.error ? (
                  importResult.error
                ) : (
                  <>
                    <strong>Import termine !</strong><br />
                    {importResult.imported} entrees importees
                    {importResult.errors > 0 && `, ${importResult.errors} erreurs`}
                  </>
                )}
              </Alert>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportDialog(false)}>Fermer</Button>
          <Button
            variant="contained"
            onClick={handleImport}
            disabled={!importFile || importing}
            startIcon={importing ? <CircularProgress size={20} /> : <UploadIcon />}
          >
            {importing ? 'Import en cours...' : 'Importer'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default ServerObsolescence;
