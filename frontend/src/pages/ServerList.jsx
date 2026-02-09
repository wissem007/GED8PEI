import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, FormControl, InputLabel, Select,
  MenuItem, Button, IconButton, Chip, CircularProgress, Pagination
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import {
  Search as SearchIcon, Refresh as RefreshIcon, Add as AddIcon,
  Visibility as ViewIcon, Edit as EditIcon, Warning as WarningIcon
} from '@mui/icons-material';
import { serversAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

function ServerList() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [servers, setServers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);

  // Filtres
  const [search, setSearch] = useState('');
  const [environment, setEnvironment] = useState('');
  const [site, setSite] = useState('');
  const [type, setType] = useState('');

  // Options de filtres
  const [environments, setEnvironments] = useState([]);
  const [sites, setSites] = useState([]);

  // Pagination
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  useEffect(() => {
    loadFilters();
    loadServers();
  }, []);

  useEffect(() => {
    loadServers();
  }, [page, pageSize, environment, site, type]);

  const loadFilters = async () => {
    try {
      const [envRes, siteRes] = await Promise.all([
        serversAPI.getEnvironments(),
        serversAPI.getSites()
      ]);
      setEnvironments(envRes.data);
      setSites(siteRes.data);
    } catch (err) {
      console.error('Erreur chargement filtres:', err);
    }
  };

  const loadServers = async () => {
    try {
      setLoading(true);
      const params = {
        page,
        size: pageSize,
        ...(environment && { environment }),
        ...(site && { site }),
        ...(type && { type }),
        ...(search && { search })
      };
      const response = await serversAPI.getAll(params);
      setServers(response.data.content || []);
      setTotalElements(response.data.totalElements || 0);
    } catch (err) {
      console.error('Erreur chargement serveurs:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(0);
    loadServers();
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const getTypeColor = (serverType) => {
    switch (serverType) {
      case 'SERVER': return 'primary';
      case 'NAS': return 'success';
      case 'DBAAS': return 'warning';
      default: return 'default';
    }
  };

  const getEnvColor = (env) => {
    switch (env?.toUpperCase()) {
      case 'PROD': return 'error';
      case 'PREPROD': return 'warning';
      case 'RECETTE': return 'info';
      case 'DEV': return 'success';
      default: return 'default';
    }
  };

  // Fonction pour déterminer le statut basé sur la date de MAJ
  const getStatus = (lastUpdate) => {
    if (!lastUpdate) return 'A planifier';
    const now = new Date();
    const updateDate = new Date(lastUpdate);
    const diffDays = Math.floor((now - updateDate) / (1000 * 60 * 60 * 24));
    if (diffDays <= 90) return 'A jour';
    if (diffDays <= 180) return 'A planifier';
    return 'En retard';
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'A jour': return 'success';
      case 'A planifier': return 'warning';
      case 'En retard': return 'error';
      default: return 'default';
    }
  };

  // Calculer prochaine MAJ (3 mois après dernière)
  const getNextUpdate = (lastUpdate) => {
    if (!lastUpdate) return 'A planifier';
    try {
      const date = new Date(lastUpdate);
      date.setMonth(date.getMonth() + 3);
      return date.toLocaleDateString('fr-FR');
    } catch {
      return 'N/A';
    }
  };

  const columns = [
    {
      field: 'environmentName',
      headerName: 'Environnement',
      width: 120,
      renderCell: (params) => (
        params.value ? (
          <Chip
            label={params.value}
            color={getEnvColor(params.value)}
            size="small"
          />
        ) : '-'
      )
    },
    {
      field: 'serverType',
      headerName: 'Type',
      width: 100,
      renderCell: (params) => {
        if (!params.value) return '-';
        const label = params.value === 'SERVER' ? 'Serveur' :
                      params.value === 'NAS' ? 'NAS' :
                      params.value === 'DBAAS' ? 'DBaaS' : params.value;
        return (
          <Chip
            label={label}
            color={getTypeColor(params.value)}
            size="small"
            variant="outlined"
          />
        );
      }
    },
    {
      field: 'infrastructureType',
      headerName: 'Infrastructure',
      width: 100,
      renderCell: (params) => {
        if (!params.value) return '-';
        const label = params.value === 'PHYSICAL' ? 'Physique' : 'VM';
        const color = params.value === 'PHYSICAL' ? 'secondary' : 'info';
        return (
          <Chip
            label={label}
            color={color}
            size="small"
            variant="outlined"
          />
        );
      }
    },
    {
      field: 'resourceName',
      headerName: 'Nom Ressource',
      flex: 1,
      minWidth: 180
    },
    {
      field: 'hostname',
      headerName: 'Hostname',
      flex: 1,
      minWidth: 200
    },
    {
      field: 'ipFront',
      headerName: 'IP Front',
      width: 130
    },
    {
      field: 'ipAdmin',
      headerName: 'IP Admin',
      width: 130,
      renderCell: (params) => params.value || '-'
    },
    {
      field: 'dataCenter',
      headerName: 'Data Center',
      width: 100,
      renderCell: (params) => params.value || '-'
    },
    {
      field: 'siteCode',
      headerName: 'Site PEI',
      width: 140,
      renderCell: (params) => params.value || '-'
    },
    {
      field: 'softwareCount',
      headerName: 'Logiciels',
      width: 150,
      renderCell: (params) => {
        const count = params.value || 0;
        const needsUpdate = params.row.softwareNeedingUpdate || 0;
        const software = params.row.installedSoftware || [];

        if (count === 0) {
          return (
            <Typography variant="caption" color="text.secondary">
              Non renseigne
            </Typography>
          );
        }

        // Afficher les 2 premiers logiciels + compteur si plus
        const displaySoftware = software.slice(0, 2);
        const remaining = count - 2;

        return (
          <Box>
            <Box display="flex" alignItems="center" gap={0.5} flexWrap="wrap">
              {displaySoftware.map((sw, idx) => (
                <Chip
                  key={idx}
                  label={`${sw.softwareName}${sw.currentVersion ? ' ' + sw.currentVersion : ''}`}
                  size="small"
                  variant="outlined"
                  sx={{ maxWidth: 120, fontSize: '0.7rem' }}
                />
              ))}
              {remaining > 0 && (
                <Chip label={`+${remaining}`} size="small" color="primary" />
              )}
            </Box>
            {needsUpdate > 0 && (
              <Chip
                icon={<WarningIcon sx={{ fontSize: '0.8rem' }} />}
                label={`${needsUpdate} MAJ`}
                size="small"
                color="warning"
                sx={{ mt: 0.5, fontSize: '0.65rem' }}
              />
            )}
          </Box>
        );
      }
    },
    {
      field: 'lastAdminUpdate',
      headerName: 'Derniere MAJ',
      width: 115,
      renderCell: (params) => {
        if (!params.value) return '-';
        try {
          const date = new Date(params.value);
          return date.toLocaleDateString('fr-FR');
        } catch {
          return params.value;
        }
      }
    },
    {
      field: 'nextUpdate',
      headerName: 'Prochaine MAJ',
      width: 115,
      renderCell: (params) => getNextUpdate(params.row.lastAdminUpdate)
    },
    {
      field: 'status',
      headerName: 'Statut',
      width: 100,
      renderCell: (params) => {
        const status = getStatus(params.row.lastAdminUpdate);
        return (
          <Chip
            label={status}
            color={getStatusColor(status)}
            size="small"
          />
        );
      }
    },
    {
      field: 'notes',
      headerName: 'Commentaires',
      width: 150,
      renderCell: (params) => params.value || '-'
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 100,
      sortable: false,
      renderCell: (params) => (
        <Box>
          <IconButton
            size="small"
            onClick={() => navigate(`/servers/${params.row.id}`)}
            title="Voir"
          >
            <ViewIcon />
          </IconButton>
          {isAuthenticated() && (
            <IconButton
              size="small"
              onClick={() => navigate(`/servers/${params.row.id}/edit`)}
              title="Modifier"
            >
              <EditIcon />
            </IconButton>
          )}
        </Box>
      )
    }
  ];

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" fontWeight="bold" color="primary">
          INVENTAIRE COMPLET DES SERVEURS GED-PEI
        </Typography>
        {isAuthenticated() && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate('/servers/new')}
          >
            Ajouter
          </Button>
        )}
      </Box>

      {/* Filtres */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
          <TextField
            size="small"
            placeholder="Rechercher..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyPress={handleKeyPress}
            InputProps={{
              startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />
            }}
            sx={{ minWidth: 250 }}
          />

          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Environnement</InputLabel>
            <Select
              value={environment}
              label="Environnement"
              onChange={(e) => { setEnvironment(e.target.value); setPage(0); }}
            >
              <MenuItem value="">Tous</MenuItem>
              {environments.map((env) => (
                <MenuItem key={env} value={env}>{env}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Site</InputLabel>
            <Select
              value={site}
              label="Site"
              onChange={(e) => { setSite(e.target.value); setPage(0); }}
            >
              <MenuItem value="">Tous</MenuItem>
              {sites.map((s) => (
                <MenuItem key={s} value={s}>{s}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Type</InputLabel>
            <Select
              value={type}
              label="Type"
              onChange={(e) => { setType(e.target.value); setPage(0); }}
            >
              <MenuItem value="">Tous</MenuItem>
              <MenuItem value="SERVER">Serveur</MenuItem>
              <MenuItem value="NAS">NAS</MenuItem>
              <MenuItem value="DBAAS">DBaaS</MenuItem>
            </Select>
          </FormControl>

          <Button variant="outlined" onClick={handleSearch}>
            Rechercher
          </Button>

          <IconButton onClick={loadServers}>
            <RefreshIcon />
          </IconButton>
        </Box>
      </Paper>

      {/* Tableau */}
      <Paper sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={servers}
          columns={columns}
          loading={loading}
          pageSizeOptions={[10, 20, 50]}
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(model) => {
            setPage(model.page);
            setPageSize(model.pageSize);
          }}
          rowCount={totalElements}
          paginationMode="server"
          disableRowSelectionOnClick
          sx={{
            '& .MuiDataGrid-cell:focus': { outline: 'none' },
            '& .MuiDataGrid-row:hover': { bgcolor: 'action.hover' }
          }}
        />
      </Paper>
    </Box>
  );
}

export default ServerList;
