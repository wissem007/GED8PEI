import { useState, useEffect, useCallback } from 'react';
import {
  Box, Paper, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, TablePagination, TextField, FormControl, InputLabel,
  Select, MenuItem, Chip, Button, Alert, CircularProgress, Card, CardContent,
  Grid, IconButton, Tooltip, Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import {
  Upload as UploadIcon, Refresh as RefreshIcon, Search as SearchIcon,
  CheckCircle as CheckIcon, Warning as WarningIcon, Error as ErrorIcon,
  Info as InfoIcon, Link as LinkIcon, OpenInNew as OpenInNewIcon
} from '@mui/icons-material';
import { softwareVersionsAPI } from '../services/api';

const statusColors = {
  PRECONISEE: { bg: '#e8f5e9', color: '#2e7d32', label: 'Preconisee' },
  TRAJECTOIRE_PRECONISEE: { bg: '#e3f2fd', color: '#1565c0', label: 'Trajectoire' },
  TOLEREE: { bg: '#fff3e0', color: '#ef6c00', label: 'Toleree' },
  INTERDITE: { bg: '#ffebee', color: '#c62828', label: 'Interdite' },
  INTERDITE_CYBER: { bg: '#fce4ec', color: '#ad1457', label: 'Interdite Cyber' },
};

function SoftwareVersions() {
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState({ softwareName: '', version: '', status: '' });
  const [statuses, setStatuses] = useState([]);
  const [stats, setStats] = useState({});
  const [softwareNames, setSoftwareNames] = useState([]);
  const [importDialog, setImportDialog] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);

  const fetchVersions = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: rowsPerPage,
        sortBy: 'softwareName',
        sortDir: 'asc',
        ...(filters.softwareName && { softwareName: filters.softwareName }),
        ...(filters.version && { version: filters.version }),
        ...(filters.status && { status: filters.status }),
      };
      const response = await softwareVersionsAPI.getAll(params);
      setVersions(response.data.content || []);
      setTotalElements(response.data.totalElements || 0);
    } catch (err) {
      console.error('Erreur chargement versions:', err);
      setError('Erreur lors du chargement des versions');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, filters]);

  const fetchMetadata = useCallback(async () => {
    try {
      const [statusesRes, statsRes, namesRes] = await Promise.all([
        softwareVersionsAPI.getStatuses(),
        softwareVersionsAPI.getStats(),
        softwareVersionsAPI.getSoftwareNames(),
      ]);
      setStatuses(statusesRes.data || []);
      setStats(statsRes.data || {});
      setSoftwareNames(namesRes.data || []);
    } catch (err) {
      console.error('Erreur chargement metadata:', err);
    }
  }, []);

  useEffect(() => {
    fetchVersions();
  }, [fetchVersions]);

  useEffect(() => {
    fetchMetadata();
  }, [fetchMetadata]);

  const handleFilterChange = (field, value) => {
    setFilters(prev => ({ ...prev, [field]: value }));
    setPage(0);
  };

  const handleImport = async () => {
    if (!importFile) return;

    setImporting(true);
    setImportResult(null);
    try {
      const response = await softwareVersionsAPI.importCsv(importFile);
      setImportResult(response.data);
      fetchVersions();
      fetchMetadata();
    } catch (err) {
      console.error('Erreur import:', err);
      setImportResult({ error: err.response?.data?.message || 'Erreur lors de l\'import' });
    } finally {
      setImporting(false);
    }
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      setImportFile(file);
      setImportResult(null);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  };

  const getStatusChip = (status) => {
    const config = statusColors[status] || { bg: '#f5f5f5', color: '#757575', label: status };
    return (
      <Chip
        label={config.label}
        size="small"
        sx={{
          bgcolor: config.bg,
          color: config.color,
          fontWeight: 'medium',
        }}
      />
    );
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'PRECONISEE':
      case 'TRAJECTOIRE_PRECONISEE':
        return <CheckIcon sx={{ color: '#2e7d32' }} />;
      case 'TOLEREE':
        return <WarningIcon sx={{ color: '#ef6c00' }} />;
      case 'INTERDITE':
      case 'INTERDITE_CYBER':
        return <ErrorIcon sx={{ color: '#c62828' }} />;
      default:
        return <InfoIcon />;
    }
  };

  // Parse et affiche les liens utiles
  const renderUsefulLinks = (usefulLinks) => {
    if (!usefulLinks) return '-';

    try {
      const links = JSON.parse(usefulLinks);
      if (!Array.isArray(links) || links.length === 0) return '-';

      return (
        <Box display="flex" gap={0.5} flexWrap="wrap">
          {links.map((link, index) => (
            <Tooltip key={index} title={link.label || link.url}>
              <IconButton
                size="small"
                href={link.url}
                target="_blank"
                rel="noopener noreferrer"
                sx={{
                  p: 0.5,
                  color: 'primary.main',
                  '&:hover': { bgcolor: 'primary.light', color: 'white' }
                }}
              >
                <OpenInNewIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ))}
          <Typography variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>
            ({links.length})
          </Typography>
        </Box>
      );
    } catch (e) {
      // Si ce n'est pas du JSON valide, afficher un lien simple si c'est une URL
      if (usefulLinks.startsWith('http')) {
        return (
          <Tooltip title="Ouvrir le lien">
            <IconButton
              size="small"
              href={usefulLinks}
              target="_blank"
              rel="noopener noreferrer"
              sx={{ p: 0.5, color: 'primary.main' }}
            >
              <LinkIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        );
      }
      return '-';
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight="bold">
          Versions de Logiciels
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => { fetchVersions(); fetchMetadata(); }}
            sx={{ mr: 1 }}
          >
            Actualiser
          </Button>
          <Button
            variant="contained"
            startIcon={<UploadIcon />}
            onClick={() => setImportDialog(true)}
          >
            Importer
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Statistiques */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {Object.entries(stats).map(([label, count]) => (
          <Grid item xs={6} sm={4} md={2} key={label}>
            <Card variant="outlined">
              <CardContent sx={{ textAlign: 'center', py: 1 }}>
                <Typography variant="h5" fontWeight="bold">{count}</Typography>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Filtres */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>Logiciel</InputLabel>
              <Select
                value={filters.softwareName}
                label="Logiciel"
                onChange={(e) => handleFilterChange('softwareName', e.target.value)}
              >
                <MenuItem value="">Tous</MenuItem>
                {softwareNames.map((name) => (
                  <MenuItem key={name} value={name}>{name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              size="small"
              label="Version"
              value={filters.version}
              onChange={(e) => handleFilterChange('version', e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ color: 'text.secondary', mr: 1 }} />,
              }}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
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
        </Grid>
      </Paper>

      {/* Table */}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.100' }}>
              <TableCell><strong>Logiciel</strong></TableCell>
              <TableCell><strong>Version Roadmap</strong></TableCell>
              <TableCell><strong>Version Package</strong></TableCell>
              <TableCell><strong>Statut</strong></TableCell>
              <TableCell><strong>MAJ Statut</strong></TableCell>
              <TableCell><strong>Fin Support Initial</strong></TableCell>
              <TableCell><strong>Fin Support Etendu</strong></TableCell>
              <TableCell><strong>Fin Support Cyber</strong></TableCell>
              <TableCell><strong>Liens</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={9} align="center" sx={{ py: 4 }}>
                  <CircularProgress />
                </TableCell>
              </TableRow>
            ) : versions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    Aucune version trouvee
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              versions.map((v) => (
                <TableRow key={v.id} hover>
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {getStatusIcon(v.status)}
                      <strong>{v.softwareName}</strong>
                    </Box>
                  </TableCell>
                  <TableCell>{v.version}</TableCell>
                  <TableCell>
                    {v.packageVersion ? (
                      <Chip label={v.packageVersion} size="small" variant="outlined" color="primary" />
                    ) : (
                      <Typography variant="caption" color="text.secondary">-</Typography>
                    )}
                  </TableCell>
                  <TableCell>{getStatusChip(v.status)}</TableCell>
                  <TableCell>{formatDate(v.statusUpdateDate)}</TableCell>
                  <TableCell>{formatDate(v.initialSupportEndDate)}</TableCell>
                  <TableCell>{formatDate(v.extendedSupportEndDate)}</TableCell>
                  <TableCell>{formatDate(v.cyberSupportEndDate)}</TableCell>
                  <TableCell>{renderUsefulLinks(v.usefulLinks)}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50, 100]}
          labelRowsPerPage="Lignes par page"
        />
      </TableContainer>

      {/* Dialog Import */}
      <Dialog open={importDialog} onClose={() => setImportDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Importer des versions de logiciels</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <Alert severity="info" sx={{ mb: 2 }}>
              <Typography variant="body2">
                <strong>Fichier attendu :</strong> export Sipedia CSR, en .xlsx ou .csv.<br />
                Le classeur peut etre depose tel quel : l'onglet <em>CSR - Detail des versions</em>
                est retenu automatiquement.<br />
                Colonnes lues : Libelle de la solution, Version Roadmap, Statut de la version Roadmap,
                Version package, dates de fin de support (initial, etendu, cyber).
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
                accept=".csv,.xls,.xlsx"
                onChange={handleFileChange}
                style={{ display: 'none' }}
              />
              <UploadIcon sx={{ fontSize: 48, color: 'grey.500', mb: 1 }} />
              <Typography>
                {importFile ? importFile.name : 'Cliquez pour selectionner un fichier (.xlsx ou .csv)'}
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
                    {importResult.imported} importes, {importResult.updated} mis a jour
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

export default SoftwareVersions;
